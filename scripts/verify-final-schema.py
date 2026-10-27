"""Verify final MySQL schema in a newly created disposable database; never uses the app DB.

Requires the existing Compose MySQL service. No API/MCP/LLM process is started.
Only the database created by this invocation is dropped in cleanup.
"""
import json
import subprocess
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DIRECTORY = ROOT / "apps/api/src/main/resources/db/migration"


def main():
    container = subprocess.run(
        ["docker", "compose", "-f", str(ROOT / "infra/docker-compose.yml"), "ps", "-q", "mysql"],
        text=True, capture_output=True, check=True,
    ).stdout.strip()
    if not container or "\n" in container:
        raise RuntimeError("Start the single Compose MySQL service before this check.")

    database = "wonnie_schema_check_" + uuid.uuid4().hex[:12]
    created = False
    checks = []

    def mysql(sql, selected=None, expected_error=None):
        # Resolve the existing container's root password inside the container, not in output/argv.
        command = ["docker", "exec", "-i", container, "sh", "-c",
                   'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --protocol=socket --user=root '
                   '--default-character-set=utf8mb4 --batch --raw --skip-column-names "$@"', "mysql"]
        if selected:
            command.append(selected)
        result = subprocess.run(command, input=sql, text=True, capture_output=True)
        if expected_error:
            if result.returncode == 0 or f"ERROR {expected_error} " not in result.stderr:
                raise AssertionError(f"Expected MySQL {expected_error}: {result.stdout} {result.stderr}")
        elif result.returncode:
            raise RuntimeError(result.stderr)
        return result.stdout.strip()

    def equal(label, query, expected):
        actual = mysql(query, database)
        if actual != expected:
            raise AssertionError(f"{label}: expected {expected!r}, got {actual!r}")
        checks.append(label)

    def rejected(label, query, code):
        mysql(query, database, expected_error=code)
        checks.append(label)

    try:
        version = mysql("SELECT VERSION();")
        mysql(f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
        created = True
        for name in ["V1__wonnie_schema.sql", "V2__wonnie_seed_demo.sql"]:
            mysql((DIRECTORY / name).read_text(), database)
        equal("32 tables", "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';", "32")
        equal("2 views", "SELECT COUNT(*) FROM information_schema.views WHERE table_schema=DATABASE();", "2")
        equal("no duplicate legacy stores", "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('assistant_message','finance_customer','finance_card','finance_transaction','merchant_directory');", "0")
        equal("demo identity", "SELECT id FROM customer;", "cust-kimwoori")
        equal("184 transactions", "SELECT COUNT(*) FROM card_transaction;", "184")
        equal("183 approved and 1 canceled", "SELECT status,COUNT(*) FROM card_transaction GROUP BY status ORDER BY status;", "APPROVED\t183\nCANCELED\t1")
        equal("7 active and 1 trial", "SELECT status,COUNT(*) FROM subscription GROUP BY status ORDER BY status;", "ACTIVE\t7\nTRIAL\t1")
        equal("confirmed and projected monthly totals", "SELECT active_count,monthly_total,trial_count,projected_monthly_total FROM v_subscription_overview;", "7\t61300.00\t1\t69190.00")
        equal("KST previous-month spend", "SELECT spend_amount FROM card_monthly_stat WHERE card_id='card-1234' AND ym='2026-09';", "470000.00")
        equal("KST current-month spend", "SELECT spend_amount FROM card_monthly_stat WHERE card_id='card-1234' AND ym='2026-10';", "412000.00")
        equal("demo-clock spend view", "SELECT prev_month_spend,this_month_spend FROM v_card_spend_now WHERE card_id='card-1234';", "470000.00\t412000.00")
        equal("all 10 consents initially off", "SELECT COUNT(*),SUM(agreed) FROM consent;", "10\t0")
        equal("KRW evidence normalized", "SELECT COUNT(*) FROM card_transaction WHERE currency='KRW' AND (original_amount IS NULL OR original_amount<>amount OR exchange_rate<>1);", "0")
        equal("unknown usage remains unknown", "SELECT COUNT(*) FROM subscription_context WHERE usage_count30_days IS NULL AND contract_verified_on IS NULL AND family_covered IS NULL;", "8")
        equal("no fabricated exact income", "SELECT monthly_income IS NULL FROM customer;", "1")
        equal("unconfirmed trial evidence", "SELECT trial_ends_on,trial_end_confirmed,contract_status FROM subscription_context WHERE id='sub-coupangwow-trial';", "2026-11-03\t0\tUNKNOWN")
        equal("runtime stores initially empty", "SELECT (SELECT COUNT(*) FROM chat_message)+(SELECT COUNT(*) FROM suggestion)+(SELECT COUNT(*) FROM outbox_event)+(SELECT COUNT(*) FROM subscription_price_observation)+(SELECT COUNT(*) FROM wonnie_event_log);", "0")

        mysql("""
            INSERT INTO assistant_conversation (id,customer_id) VALUES ('verify-conversation','cust-kimwoori');
            INSERT INTO chat_message (id,conversation_id,customer_id,role,source,content) VALUES
              ('verify-user','verify-conversation','cust-kimwoori','USER','USER_INPUT','test question');
            INSERT INTO chat_message (id,conversation_id,customer_id,role,source,content,source_key) VALUES
              ('verify-notice','verify-conversation','cust-kimwoori','ASSISTANT','PRICE_INCREASE','test notice','verify-price-event');
        """, database)
        equal("unread notice", "SELECT COUNT(*) FROM chat_message WHERE conversation_id='verify-conversation' AND role='ASSISTANT' AND read_at IS NULL;", "1")
        mysql("UPDATE chat_message SET read_at=UTC_TIMESTAMP(6) WHERE id='verify-notice';", database)
        equal("mark notice read", "SELECT COUNT(*) FROM chat_message WHERE conversation_id='verify-conversation' AND role='ASSISTANT' AND read_at IS NULL;", "0")
        rejected("duplicate notice blocked", "INSERT INTO chat_message (id,conversation_id,customer_id,role,source,content,source_key) VALUES ('verify-repeat','verify-conversation','cust-kimwoori','ASSISTANT','PRICE_INCREASE','repeat','verify-price-event');", 1062)
        rejected("role/source mismatch blocked", "INSERT INTO chat_message (id,conversation_id,customer_id,role,source,content) VALUES ('verify-role','verify-conversation','cust-kimwoori','USER','PRICE_INCREASE','invalid');", 3819)
        mysql("INSERT INTO customer (id,name,birth_date,region_sido,region_sigungu,occupation,income_band,member_since) VALUES ('verify-other','검증 고객','1990-01-01','서울특별시','마포구','OTHER','30M_40M','2021-01-01');", database)
        rejected("cross-customer conversation blocked", "INSERT INTO chat_message (id,conversation_id,customer_id,role,source,content) VALUES ('verify-wrong-owner','verify-conversation','verify-other','USER','USER_INPUT','invalid');", 1452)

        mysql("INSERT INTO subscription_price_observation (id,event_id,subscription_id,previous_amount,observed_amount,observed_at,applied,price_increased,currency,original_amount,exchange_rate,change_reason) VALUES ('verify-observation','verify-payment-event','sub-tving',5500,6500,UTC_TIMESTAMP(6),TRUE,TRUE,'KRW',6500,1,'ORIGINAL_AMOUNT_INCREASE');", database)
        rejected("price-event replay blocked", "INSERT INTO subscription_price_observation (id,event_id,subscription_id,previous_amount,observed_amount,observed_at,applied,price_increased) VALUES ('verify-replay','verify-payment-event','sub-tving',5500,6500,UTC_TIMESTAMP(6),TRUE,TRUE);", 1062)
        mysql("INSERT INTO outbox_event (id,aggregate_type,aggregate_id,event_type,payload,deduplication_key) VALUES ('verify-outbox','SUBSCRIPTION','sub-tving','PRICE_OBSERVED','{\"eventId\":\"verify-payment-event\"}','verify-outbox-event');", database)
        rejected("outbox replay blocked", "INSERT INTO outbox_event (id,aggregate_type,aggregate_id,event_type,payload,deduplication_key) VALUES ('verify-outbox-repeat','SUBSCRIPTION','sub-tving','PRICE_OBSERVED','{}','verify-outbox-event');", 1062)
        rejected("invalid outbox JSON blocked", "INSERT INTO outbox_event (id,aggregate_type,aggregate_id,event_type,payload) VALUES ('verify-bad-json','SUBSCRIPTION','sub-tving','PRICE_OBSERVED','not JSON');", 3819)
        mysql("UPDATE outbox_event SET status='PUBLISHED',published_at=UTC_TIMESTAMP(6) WHERE id='verify-outbox';", database)
        equal("outbox publication state", "SELECT status FROM outbox_event WHERE id='verify-outbox';", "PUBLISHED")
        mysql("INSERT INTO subscription (id,customer_id,card_id,merchant_id,service_id,display_name,category,billing_cycle,amount,charge_day,next_charge_on,status,detection_confidence) VALUES ('verify-yearly','cust-kimwoori','card-1234','mch-watcha','svc-watcha','검증 연간권','OTT','YEARLY',600000,1,'2027-10-01','ACTIVE',1);", database)
        equal("yearly subscription monthly conversion", "SELECT monthly_total,projected_monthly_total FROM v_subscription_overview WHERE customer_id='cust-kimwoori';", "111300.00\t119190.00")
        rejected("single demo clock enforced", "INSERT INTO demo_clock (id,today) VALUES (2,'2026-10-27');", 3819)
        report = {"status": "PASS", "mysqlVersion": version, "checksPassed": len(checks),
                  "checks": checks, "applicationDatabaseChanged": False}
    finally:
        if created:
            assert database.startswith("wonnie_schema_check_") and len(database) == 32
            mysql(f"DROP DATABASE `{database}`;")
    report["temporaryDatabaseRemoved"] = True
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
