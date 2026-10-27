"""Boot the actual docker-profile API twice against a disposable MySQL DB/user.

The running app database, Docker volumes and user-owned API/MCP processes are untouched.
Requires an existing MySQL Compose service and a freshly built API bootJar.
"""
import json, os, shutil, socket, subprocess, tempfile, time, urllib.request, uuid
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


def main():
    container=subprocess.check_output(['docker','compose','-f',str(ROOT/'infra/docker-compose.yml'),'ps','-q','mysql'],text=True).strip()
    if not container or '\n' in container:raise RuntimeError('A single running Compose MySQL service is required.')
    token=uuid.uuid4().hex[:12]
    database='wonnie_boot_check_'+token
    username='wonnie_check_'+token
    password=uuid.uuid4().hex
    created_db=created_user=False
    processes=[];logs=[]
    jar=next(p for p in (ROOT/'apps/api/build/libs').glob('*.jar') if not p.name.endswith('-plain.jar'))
    java=str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')

    def sql(query):
        command=['docker','exec','-i',container,'sh','-c',
            'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --protocol=socket -uroot --batch --raw --skip-column-names']
        result=subprocess.run(command,input=query,text=True,capture_output=True)
        if result.returncode:raise RuntimeError(result.stderr.replace(password,'[REDACTED]'))
        return result.stdout.strip()

    def request(port,path,body=None):
        data=None if body is None else json.dumps(body).encode()
        req=urllib.request.Request(f'http://127.0.0.1:{port}{path}',data=data,headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(req,timeout=20) as response:return json.load(response)

    def start():
        with socket.socket() as listener:
            listener.bind(('127.0.0.1',0));port=listener.getsockname()[1]
        env=dict(os.environ,MYSQL_DATABASE=database,MYSQL_USER=username,MYSQL_PASSWORD=password)
        log=tempfile.TemporaryFile(mode='w+');logs.append(log)
        process=subprocess.Popen([java,'-jar',str(jar),'--spring.profiles.active=docker',f'--server.port={port}',
            '--app.messaging.enabled=false','--spring.rabbitmq.listener.simple.auto-startup=false',
            '--management.health.rabbit.enabled=false'],env=env,stdout=log,stderr=subprocess.STDOUT,cwd=ROOT)
        processes.append(process)
        deadline=time.monotonic()+50
        while time.monotonic()<deadline:
            if process.poll() is not None:raise RuntimeError('Docker-profile API exited during startup.')
            try:
                if request(port,'/actuator/health').get('status')=='UP':return process,port
            except Exception:pass
            time.sleep(.25)
        raise TimeoutError('Docker-profile API did not become healthy.')

    def stop(process):
        if process.poll() is None:
            process.terminate()
            try:process.wait(timeout=10)
            except subprocess.TimeoutExpired:process.kill();process.wait()

    try:
        sql(f'CREATE DATABASE `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;');created_db=True
        sql(f"CREATE USER '{username}'@'%' IDENTIFIED BY '{password}';");created_user=True
        sql(f"GRANT ALL PRIVILEGES ON `{database}`.* TO '{username}'@'%';")
        process,port=start()
        report=request(port,'/api/finance/subscriptions')
        assert len(report['data']['subscriptions'])==7,report
        assert report['data']['confirmedMonthlyCost']==61300,report
        assert report['data']['projectedMonthlyCostAfterTrials']==69190,report
        dashboard=request(port,'/api/dashboard')
        assert dashboard['activeCount']==7 and dashboard['totalMonthlyCost']==61300,dashboard
        for topic in ['payment-readiness','price-changes','overlaps','renewals','cancellations','usage','card-recommendations','customer-context','card-schedule','alerts']:
            assert request(port,'/api/finance/'+topic)['dataSource']=='DATABASE',topic
        assert request(port,'/api/finance/merchant?name=APPLE.COM%2FBILL')['data']['verifiedSubscription'] is False
        exchange=request(port,'/api/assistant/messages',{'message':'내 구독 내역 알려줘'})
        assert exchange['userMessage']['content']=='내 구독 내역 알려줘',exchange
        assert len(request(port,'/api/assistant/conversation')['messages'])==2
        # The manual scan endpoint is intentionally local-profile-only. Exercise persisted notices directly here.
        notice_sql=f"INSERT IGNORE INTO `{database}`.chat_message (id,conversation_id,customer_id,role,source,content,source_key,created_at) SELECT 'verify-notice',id,customer_id,'ASSISTANT','FINANCE_NOTICE','검증용 안내','verify-notice-key',UTC_TIMESTAMP(6) FROM `{database}`.assistant_conversation WHERE customer_id='cust-kimwoori';"
        sql(notice_sql)
        before=request(port,'/api/assistant/conversation')
        assert before['unreadCount']>0,before
        sql(notice_sql)
        assert len(request(port,'/api/assistant/conversation')['messages'])==len(before['messages'])
        now=datetime.now(ZoneInfo('Asia/Seoul'))
        observed=datetime.fromisoformat(report['data']['businessDate']).replace(hour=now.hour,minute=now.minute,second=now.second,microsecond=now.microsecond)+timedelta(seconds=1)
        event={'eventId':'verify-price-event','subscriptionId':'sub-tving','amount':6500,
               'observedAt':observed.isoformat(),'currency':'KRW','originalAmount':6500,'exchangeRate':1}
        request(port,'/api/internal/subscription-price-observations',event)
        after_price=request(port,'/api/assistant/conversation')
        assert any(m['source']=='PRICE_INCREASE' and '6500' in m['content'] for m in after_price['messages']),after_price
        request(port,'/api/internal/subscription-price-observations',event)
        assert len(request(port,'/api/assistant/conversation')['messages'])==len(after_price['messages'])
        assert request(port,'/api/dashboard')['totalMonthlyCost']==62300
        before=after_price
        request(port,'/api/assistant/conversation/read',{})
        assert request(port,'/api/assistant/unread-count')['unreadCount']==0
        stop(process)
        process,port=start()
        assert len(request(port,'/api/assistant/conversation')['messages'])==len(before['messages'])
        assert request(port,'/api/assistant/unread-count')['unreadCount']==0
        assert sql(f'SELECT COUNT(*) FROM `{database}`.card_transaction;')=='184'
        versions=sql(f'SELECT version,success FROM `{database}`.flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank;')
        assert versions=='1\t1\n2\t1\n3\t1',versions
        result={'status':'PASS','profile':'docker','jpaValidation':'PASS','flyway':versions.splitlines(),
            'activeSubscriptions':7,'activeMonthlyTotal':61300,'projectedWithTrials':69190,
            'restartPreservesChatAndReadState':True,'seedNotDuplicated':True,'applicationDatabaseChanged':False}
        result['priceObservationUpdateAndReplay']='PASS'
    except Exception:
        for log in logs:
            log.flush();log.seek(0);body=log.read().replace(password,'[REDACTED]')
            print(body[-12000:])
        raise
    finally:
        for process in processes:stop(process)
        for log in logs:log.close()
        if created_user:
            assert username.startswith('wonnie_check_')
            sql(f"DROP USER '{username}'@'%';")
        if created_db:
            assert database.startswith('wonnie_boot_check_')
            sql(f'DROP DATABASE `{database}`;')
    result['temporaryDatabaseAndUserRemoved']=True
    print(json.dumps(result,ensure_ascii=False,indent=2))


if __name__=='__main__':main()
