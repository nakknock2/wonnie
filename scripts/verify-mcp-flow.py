"""Verify separate API/MCP processes using a local OpenAI HTTP stub. No paid API calls.
Build both bootJar targets before running this script.
"""
import json, os, shutil, socket, subprocess, tempfile, threading, time, urllib.request
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
root=Path(__file__).resolve().parents[1]
requests=[]
class Model(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def do_POST(self):
        body=json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        requests.append(body)
        has_tool=any(m.get('role')=='tool' for m in body.get('messages',[]))
        message={'role':'assistant','content':'샘플 데이터를 조회했어요. 월 환산 구독비는 98,200원입니다.'} if has_tool else {
            'role':'assistant','content':None,'tool_calls':[{'id':'smoke-tool','type':'function','function':{'name':'get_subscriptions','arguments':'{}'}}]}
        data=json.dumps({'id':'chatcmpl-smoke','object':'chat.completion','created':1,'model':'gpt-5.6-terra','choices':[{'index':0,'message':message,'finish_reason':'stop' if has_tool else 'tool_calls'}]},ensure_ascii=False).encode()
        self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(data)
def freeport():
    with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
def request(port,path,body=None):
    data=None if body is None else json.dumps(body).encode()
    req=urllib.request.Request(f'http://127.0.0.1:{port}{path}',data=data,headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req,timeout=40) as response:return json.load(response)
apiport,mcpport=freeport(),freeport()
model=ThreadingHTTPServer(('127.0.0.1',0),Model);threading.Thread(target=model.serve_forever,daemon=True).start()
procs=[];logs=[]
java=str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')
if not java: raise SystemExit('Java 17+ is required.')
try:
    for module,args in [('api',[f'--server.port={apiport}','--spring.profiles.active=local,openai','--spring.ai.openai.api-key=test-key-not-real',
            f'--spring.ai.openai.base-url=http://127.0.0.1:{model.server_port}',f'--app.mcp.base-url=http://127.0.0.1:{mcpport}']),
            ('mcp',[f'--server.port={mcpport}',f'--app.api.base-url=http://127.0.0.1:{apiport}'])]:
        jar=next((p for p in (root/f'apps/{module}/build/libs').glob('*.jar') if not p.name.endswith('-plain.jar')),None)
        if jar is None: raise RuntimeError(f'Build apps/{module} bootJar before running the verification script.')
        log=tempfile.TemporaryFile(mode='w+');logs.append((module,log))
        procs.append(subprocess.Popen([java,'-jar',str(jar),*args],stdout=log,stderr=subprocess.STDOUT,cwd=root))
    deadline=time.time()+50
    ready=set()
    while time.time()<deadline:
        for name,port in [('api',apiport),('mcp',mcpport)]:
            if name in ready:continue
            try:
                if request(port,'/actuator/health').get('status')=='UP':ready.add(name)
            except Exception:pass
        if len(ready)==2:break
        if any(p.poll() is not None for p in procs):raise RuntimeError('server exited during startup')
        time.sleep(.3)
    assert len(ready)==2,ready
    result=request(apiport,'/api/assistant/messages',{'message':'이번 달 구독 총액 알려줘'})
    assert '98,200' in result['assistantMessage']['content'],result
    assert len(requests)==2,len(requests)
    assert len(requests[0]['tools'])==13
    assert '98200' in json.dumps(requests[1],ensure_ascii=False),requests[1]
    names=[t['function']['name'] for t in requests[0]['tools']]
    assert not any('calendar' in n or 'pause' in n for n in names),names
    scan=request(apiport,'/api/internal/finance-alerts/scan',{})
    first=request(apiport,'/api/assistant/conversation')
    request(apiport,'/api/internal/finance-alerts/scan',{})
    second=request(apiport,'/api/assistant/conversation')
    assert len(first['messages'])==len(second['messages'])
    assert second['unreadCount']>0
    request(apiport,'/api/assistant/conversation/read',{})
    assert request(apiport,'/api/assistant/unread-count')['unreadCount']==0
    report=request(apiport,'/api/finance/price-changes')
    assert any(c['reason']=='FX_CHANGE' for c in report['data']['transactionComparisons'])
    print(json.dumps({'status':'PASS','realServers':['Spring API','Spring MCP'],'model':'local HTTP stub; no external OpenAI calls',
        'toolCount':len(names),'modelHttpCalls':len(requests),'monthlyProjected':98200,'chatMessages':len(second['messages']),
        'noticeCandidates':scan['matchedNotices'],'duplicateNotices':False,'readState':'PASS'},ensure_ascii=False))
except Exception:
    for module,log in logs:
        log.flush();log.seek(0);content=log.read();print('\n'+module+' log head:\n'+content[:10000]+'\nerror lines:\n'+'\n'.join(line for line in content.splitlines() if any(x in line for x in ['ERROR','Exception','Caused by:'])))
    raise
finally:
    for p in procs:
        if p.poll() is None:
            p.terminate()
            try:p.wait(timeout=5)
            except subprocess.TimeoutExpired:p.kill();p.wait()
    model.shutdown()
    for _,log in logs:log.close()
