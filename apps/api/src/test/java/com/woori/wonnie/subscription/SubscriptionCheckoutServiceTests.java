package com.woori.wonnie.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wonnie-checkout-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("local")
@Transactional
class SubscriptionCheckoutServiceTests {
    @Autowired SubscriptionCheckoutService checkout;
    @Autowired JdbcOperations jdbc;

    @Test
    void issuesRecommendedWooriCardThenCreatesSubscriptionAndApprovedTransaction() {
        var customerId = "cust-checkout-test";
        jdbc.update("""
                INSERT INTO customer (id,name,birth_date,region_sido,region_sigungu,occupation,income_band,
                  monthly_income,easy_mode,member_since,created_at)
                VALUES (?,?,?,?,?,?,?,NULL,FALSE,?,CURRENT_TIMESTAMP)
                """, customerId, "가입 테스트", LocalDate.of(1998, 1, 1), "서울특별시", "마포구",
                "OFFICE_WORKER", "30M_40M", LocalDate.of(2024, 1, 1));

        var initial = checkout.options(customerId, "넷플릭스", "STANDARD");
        assertThat(initial.ownedCards()).isEmpty();
        assertThat(initial.recommendedCards()).extracting(SubscriptionCheckoutService.CardOption::productId)
                .containsExactly("prd-daily", "prd-routine", "prd-jeongseok2");
        assertThat(initial.recommendedCards()).allSatisfy(card -> {
            assertThat(card.issuer()).isEqualTo("우리카드");
            assertThat(card.imageKey()).startsWith("woori-");
        });

        var issued = checkout.issueCard(customerId, "prd-daily", true);
        assertThat(issued.newlyIssued()).isTrue();
        var afterIssue = checkout.options(customerId, "넷플릭스", "STANDARD");
        assertThat(afterIssue.ownedCards()).extracting(SubscriptionCheckoutService.CardOption::id)
                .containsExactly(issued.cardId());

        var requestId = UUID.randomUUID();
        var result = checkout.subscribe(customerId, requestId, "넷플릭스", "STANDARD", issued.cardId(), true);
        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.created()).isTrue();
        assertThat(result.listPrice()).isEqualByComparingTo("13500");
        assertThat(result.cardDiscount()).isEqualByComparingTo("5000");
        assertThat(result.discountedPrice()).isEqualByComparingTo("8500");
        assertThat(result.monthlySaved()).isEqualByComparingTo("5000");
        assertThat(result.recommendation().name()).isEqualTo("워니 절약 적금");
        assertThat(result.subscriptions()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("넷플릭스");
            assertThat(item.plan()).isEqualTo("스탠다드");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM card_transaction WHERE subscription_id=? AND status='APPROVED'",
                Integer.class, result.subscriptionId())).isEqualTo(1);

        var repeated = checkout.subscribe(customerId, requestId, "넷플릭스", "STANDARD", issued.cardId(), true);
        assertThat(repeated.created()).isFalse();

        var cancelled = checkout.cancel(customerId, result.subscriptionId(), true);
        assertThat(cancelled.status()).isEqualTo("CANCEL_CONFIRMED");
        assertThat(cancelled.monthlySaved()).isEqualByComparingTo("13500");
        assertThat(cancelled.subscriptions()).isEmpty();
        assertThat(cancelled.recommendation().name()).isEqualTo("워니 절약 적금");
        assertThat(jdbc.queryForObject("SELECT status FROM subscription WHERE id=?", String.class,
                result.subscriptionId())).isEqualTo("CANCEL_CONFIRMED");
    }
}
