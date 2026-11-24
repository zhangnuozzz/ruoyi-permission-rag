#!/usr/bin/env python3
"""Apply the reviewed post-bootstrap migrations to the isolated rag-local test DB."""
import hashlib
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
COMMAND = ['docker', 'compose', '-f', str(ROOT/'docker-compose.local.yml'), 'exec', '-T',
           '-e', 'MYSQL_PWD=001106', 'mariadb', 'mariadb', '-uruoyi', '--batch', '--skip-column-names', 'ry-vue-320']
MIGRATIONS = [
    'fix_rag_search_admin_context_20260711.sql',
    'refactor_core_access_model_20260929.sql',
    'refactor_permission_center_20260929.sql',
    'refactor_personnel_access_20260929.sql',
    'refactor_personnel_menu_permissions_20260929.sql',
    'add_rag_audit_query_indexes_20261029.sql',
    'add_access_time_scenarios_20261029.sql',
    'add_group_manager_20261030.sql',
    'add_governance_export_permissions_20261030.sql',
    'platform-account-security.sql',
    'platform-name-management.sql',
    'policy-condition-version.sql',
    'extend_rag_audit_query_index_20261108.sql',
    'add_dashboard_stat_indexes_20261108.sql',
]

def sql(text):
    return subprocess.check_output(COMMAND, input=text.encode(), cwd=ROOT).decode()

sql('CREATE TABLE IF NOT EXISTS rag_local_schema_migration '
    '(name VARCHAR(200) PRIMARY KEY, sha256 CHAR(64) NOT NULL, applied_at DATETIME DEFAULT CURRENT_TIMESTAMP);')
for name in MIGRATIONS:
    content = (ROOT/'sql'/name).read_bytes()
    digest = hashlib.sha256(content).hexdigest()
    existing = sql(f"SELECT sha256 FROM rag_local_schema_migration WHERE name='{name}';").strip()
    if existing:
        if existing != digest:
            raise RuntimeError(f'Applied migration changed: {name}')
        print('Already applied:', name, flush=True)
        continue
    print('Applying:', name, flush=True)
    sql(content.decode())
    sql(f"INSERT INTO rag_local_schema_migration(name,sha256) VALUES ('{name}','{digest}');")
print('Reviewed local migrations complete.', flush=True)
