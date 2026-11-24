#!/usr/bin/env python3
"""Manage this workspace's local test stack without terminating unrelated processes."""
import argparse, json, os, pathlib, signal, subprocess, time, urllib.request

PROJECT = pathlib.Path(__file__).resolve().parents[1]
RUNTIME = PROJECT.parents[1] / '.local-runtime'
STATE = RUNTIME / 'processes.json'
LOGS = RUNTIME / 'logs'
DB_URL = 'jdbc:mysql://127.0.0.1:3307/ry-vue-320?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8'
SERVICES = {
    'embedding': ([str(PROJECT/'scripts/start-local-embedding.sh')], PROJECT, 'http://127.0.0.1:8001/health'),
    'llm': ([str(PROJECT/'scripts/start-local-llm.sh')], PROJECT, 'http://127.0.0.1:8000/health'),
    'rag': ([str(RUNTIME/'jdk17/bin/java'), '-jar', 'rag_server/target/rag-server-1.0.0.jar',
             '--spring.config.additional-location=file:'+str(PROJECT/'deploy/local/rag.yml')], PROJECT, 'http://127.0.0.1:8081/'),
    'platform': ([str(RUNTIME/'jdk8/bin/java'), '-jar', 'ruoyi-admin/target/ruoyi-admin.jar',
                  '--spring.config.additional-location=file:'+str(PROJECT/'deploy/local/platform.yml'),
                  '--spring.datasource.druid.master.url='+DB_URL], PROJECT, 'http://127.0.0.1:8080/captchaImage'),
    'ui': ([str(RUNTIME/'node/bin/node'), str(PROJECT/'ruoyi-ui/node_modules/.bin/vue-cli-service'),
            'serve', '--host', '127.0.0.1', '--port', '1024'], PROJECT/'ruoyi-ui', 'http://127.0.0.1:1024/'),
}
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

def stamp(pid):
    try:
        # starttime prevents accidentally signalling a reused PID after a restart.
        fields = pathlib.Path(f'/proc/{pid}/stat').read_text().rsplit(')',1)[1].split()
        return None if fields[0] == 'Z' else fields[19]
    except (OSError, IndexError):
        return None

def healthy(url):
    try:
        with OPENER.open(url, timeout=2) as response:
            return response.status == 200
    except Exception:
        return False

def save(state):
    temp = STATE.with_suffix('.tmp')
    temp.write_text(json.dumps(state, indent=2)); temp.replace(STATE)

def compose(*args):
    subprocess.run(['docker','compose','-f',str(PROJECT/'docker-compose.local.yml'),*args], check=True, cwd=PROJECT)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('action',choices=['start','status','stop'])
    args=parser.parse_args()
    RUNTIME.mkdir(exist_ok=True); LOGS.mkdir(exist_ok=True)
    state=json.loads(STATE.read_text()) if STATE.exists() else {}
    if args.action=='status':
        compose('ps')
        for name,(_,_,url) in SERVICES.items(): print(name, 'ready' if healthy(url) else 'unavailable', url)
        return
    if args.action=='stop':
        for name in reversed(SERVICES):
            record=state.get(name,{})
            if record and stamp(record['pid'])==record['starttime']:
                os.kill(record['pid'],signal.SIGTERM)
                for _ in range(100):
                    if stamp(record['pid'])!=record['starttime']: break
                    time.sleep(0.1)
                if stamp(record['pid'])==record['starttime']:
                    raise RuntimeError(f'{name} did not stop; inspect its log before retrying')
            state.pop(name,None);save(state)
        # Keep volumes, including DB, source documents and vectors.
        compose('stop')
        return
    compose('up','-d','--no-build','--pull','never')
    for _ in range(120):
        result=subprocess.run(['docker','compose','-f',str(PROJECT/'docker-compose.local.yml'),'exec','-T',
                               'mariadb','healthcheck.sh','--connect','--innodb_initialized'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if result.returncode==0:break
        time.sleep(1)
    else:raise RuntimeError('Database not ready')
    subprocess.run(['python3',str(PROJECT/'scripts/migrate-local-db.py')],check=True)
    env=os.environ.copy();env['PATH']=str(RUNTIME/'node/bin')+':'+env['PATH']
    env['NODE_OPTIONS']='--openssl-legacy-provider'
    for name,(command,cwd,url) in SERVICES.items():
        if healthy(url): print(name,'already ready',flush=True);continue
        old=state.get(name)
        if old and stamp(old['pid'])==old['starttime']:
            raise RuntimeError(f'{name} is running but unhealthy; inspect its log')
        if command[0].endswith('.sh'):command=['bash',*command]
        with (LOGS/(name+'-server.log')).open('a') as log:
            process=subprocess.Popen(command,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        state[name]={'pid':process.pid,'starttime':stamp(process.pid)};save(state)
        for _ in range(180):
            if healthy(url):break
            if process.poll() is not None:raise RuntimeError(f'{name} exited: see {LOGS/(name+"-server.log")}')
            time.sleep(1)
        else:raise RuntimeError(f'{name} startup timed out')
        print(name,'ready',flush=True)

if __name__=='__main__':main()
