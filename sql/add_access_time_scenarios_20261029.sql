-- Run after the policy-version schema migration. Existing policy definitions are preserved.
-- New scenarios remain unbound and do not change personnel access.
START TRANSACTION;
INSERT INTO sys_policy
    (policy_code, policy_name, effect, subject_type, subject_expr, resource_expr,
     env_expr, priority, status, remark, create_by, create_time, del_flag)
VALUES
    ('ACCESS_OFFICE_DAY', '日常办公访问', '1', 'GROUP', '', '', 'time not in [08:30-18:00]', 100, '0', '适用于日间办公人员', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_RESEARCH_SHIFT', '研发延时访问', '1', 'GROUP', '', '', 'time not in [08:00-20:00]', 100, '0', '适用于研发长班人员', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_OPERATIONS_NIGHT', '运维夜班访问', '1', 'GROUP', '', '', 'time not in [20:00-08:00]', 100, '0', '适用于跨日值班人员', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_CONTRACTOR_DAY', '外协日间访问', '1', 'GROUP', '', '', 'time not in [09:00-17:00]', 100, '0', '适用于受托人员的日间工作', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_INTERNAL_FINANCE', '财务人员内部资料访问', '1', 'GROUP', '', 'level in [INTERNAL]', 'time not in [08:30-18:30]', 100, '0', '面向财务人员的内部级文档访问时段', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_INTERNAL_PERSONNEL', '人事人员内部资料访问', '1', 'GROUP', '', 'level in [INTERNAL]', 'time not in [09:00-18:00]', 100, '0', '面向人事人员的内部级文档访问时段', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_SECRET_PROJECT', '项目成员秘密资料访问', '1', 'GROUP', '', 'level in [SECRET]', 'time not in [09:00-18:00]', 100, '0', '面向项目成员的秘密级文档访问时段', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_CONFIDENTIAL_REVIEW', '机密资料查阅', '1', 'GROUP', '', 'level in [CONFIDENTIAL]', 'time not in [09:00-17:00]', 100, '0', '限定机密级资料查阅时段', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_PUBLIC_SERVICE', '公开知识服务时段', '1', 'GROUP', '', 'level in [PUBLIC]', 'time not in [06:00-23:00]', 100, '0', '限定公开知识服务开放时段', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_INTERNAL_EARLY', '内部资料早班访问', '1', 'GROUP', '', 'level in [INTERNAL]', 'time not in [06:00-14:00]', 100, '0', '适用于早班人员的内部资料查阅', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_SECRET_NIGHT', '秘密资料夜班访问', '1', 'GROUP', '', 'level in [SECRET]', 'time not in [20:00-08:00]', 100, '0', '适用于秘密级资料的夜班处理', 'scenario_init_20261029', now(), '0'),
    ('ACCESS_CONFIDENTIAL_NIGHT', '机密资料值守访问', '1', 'GROUP', '', 'level in [CONFIDENTIAL]', 'time not in [22:00-06:00]', 100, '0', '适用于机密级资料的夜间值守', 'scenario_init_20261029', now(), '0')
ON DUPLICATE KEY UPDATE policy_code = sys_policy.policy_code;

INSERT INTO sys_policy_version
    (policy_id, version_no, policy_code, policy_name, effect, subject_type, subject_expr,
     resource_expr, env_expr, priority, status, remark, change_type, change_by, change_time)
SELECT p.id, 1, p.policy_code, p.policy_name, p.effect, p.subject_type, p.subject_expr,
       p.resource_expr, p.env_expr, p.priority, p.status, p.remark, 'CREATE', p.create_by, p.create_time
FROM sys_policy p
WHERE p.create_by = 'scenario_init_20261029'
  AND p.policy_code IN ('ACCESS_OFFICE_DAY', 'ACCESS_RESEARCH_SHIFT', 'ACCESS_OPERATIONS_NIGHT', 'ACCESS_CONTRACTOR_DAY', 'ACCESS_INTERNAL_FINANCE', 'ACCESS_INTERNAL_PERSONNEL', 'ACCESS_SECRET_PROJECT', 'ACCESS_CONFIDENTIAL_REVIEW', 'ACCESS_PUBLIC_SERVICE', 'ACCESS_INTERNAL_EARLY', 'ACCESS_SECRET_NIGHT', 'ACCESS_CONFIDENTIAL_NIGHT')
  AND NOT EXISTS (SELECT 1 FROM sys_policy_version v WHERE v.policy_id = p.id)
ON DUPLICATE KEY UPDATE policy_id = sys_policy_version.policy_id;
COMMIT;
