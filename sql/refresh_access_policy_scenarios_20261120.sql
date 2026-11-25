-- 在历史场景和策略格式迁移之后执行。只更新未被修改或绑定的预置规则。
SET NAMES utf8mb4;
CREATE TEMPORARY TABLE access_scenario_refresh (
    policy_code VARCHAR(100) PRIMARY KEY,
    old_name VARCHAR(200) NOT NULL,
    resource_expr VARCHAR(500) NOT NULL,
    old_env_expr VARCHAR(500) NOT NULL,
    policy_name VARCHAR(200) NOT NULL,
    env_expr VARCHAR(500) NOT NULL,
    remark VARCHAR(500) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO access_scenario_refresh VALUES
    ('ACCESS_OFFICE_DAY', '日常办公访问', '', 'time not in [08:30-18:00]', '日间办公网访问', 'time not in [08:30-18:00] || ip not in [192.0.2.0/24]', '办公时段和办公网来源均须满足，绑定前替换示例网段。'),
    ('ACCESS_RESEARCH_SHIFT', '研发延时访问', '', 'time not in [08:00-20:00]', '研发资料来源网段限制', 'ip not in [198.51.100.0/24]', '仅允许来自研发网的访问，绑定前替换示例网段。'),
    ('ACCESS_CONTRACTOR_DAY', '外协日间访问', '', 'time not in [09:00-17:00]', '外协访问来源与风险限制', 'ip not in [203.0.113.0/27] || risk above [LOW]', '外协访问限约定网段及低风险人员，绑定前替换示例网段。'),
    ('ACCESS_INTERNAL_FINANCE', '财务人员内部资料访问', 'level in [INTERNAL]', 'time not in [08:30-18:30]', '财务内部资料来源与风险限制', 'ip not in [192.0.2.32/27] || risk above [LOW]', '内部财务资料限定专用网段及低风险人员，绑定前替换示例网段。'),
    ('ACCESS_INTERNAL_PERSONNEL', '人事人员内部资料访问', 'level in [INTERNAL]', 'time not in [09:00-18:00]', '人事内部资料风险限制', 'risk above [LOW]', '中、高风险人员不得访问内部级资料。'),
    ('ACCESS_SECRET_PROJECT', '项目成员秘密资料访问', 'level in [SECRET]', 'time not in [09:00-18:00]', '项目秘密资料来源与风险限制', 'ip not in [198.51.100.64/26] || risk above [MEDIUM]', '秘密资料限项目网段及中风险以下人员，绑定前替换示例网段。'),
    ('ACCESS_CONFIDENTIAL_REVIEW', '机密资料查阅', 'level in [CONFIDENTIAL]', 'time not in [09:00-17:00]', '机密查阅风险限制', 'risk above [LOW]', '机密查阅要求人员为低风险，基础密级与知悉组仍须满足。'),
    ('ACCESS_INTERNAL_EARLY', '内部资料早班访问', 'level in [INTERNAL]', 'time not in [06:00-14:00]', '内部资料早班网段限制', 'time not in [06:00-14:00] || ip not in [192.0.2.64/26]', '早班人员须在规定时段和网段内访问内部资料，绑定前替换示例网段。'),
    ('ACCESS_SECRET_NIGHT', '秘密资料夜班访问', 'level in [SECRET]', 'time not in [20:00-08:00]', '秘密资料夜班风险限制', 'time not in [20:00-08:00] || risk above [LOW]', '秘密资料夜间访问要求值守时段及低风险。'),
    ('ACCESS_CONFIDENTIAL_NIGHT', '机密资料值守访问', 'level in [CONFIDENTIAL]', 'time not in [22:00-06:00]', '机密值守三条件限制', 'time not in [22:00-06:00] || ip not in [203.0.113.64/26] || risk above [LOW]', '机密值守须同时满足时段、专用网段及低风险，绑定前替换示例网段。');

START TRANSACTION;

UPDATE sys_policy p
JOIN access_scenario_refresh s ON BINARY p.policy_code = BINARY s.policy_code
SET p.policy_name = s.policy_name,
    p.env_expr = s.env_expr,
    p.condition_version = 1,
    p.remark = s.remark,
    p.update_by = 'scenario_refresh_20261120',
    p.update_time = CURRENT_TIMESTAMP
WHERE BINARY p.policy_name = BINARY s.old_name
  AND BINARY COALESCE(p.resource_expr, '') = BINARY s.resource_expr
  AND BINARY p.env_expr = BINARY s.old_env_expr
  AND COALESCE(TRIM(p.subject_expr), '') = ''
  AND p.effect = '1' AND p.subject_type = 'GROUP' AND p.priority = 100
  AND p.condition_version = 0 AND p.status = '0' AND p.del_flag = '0'
  AND p.create_by IN ('admin', 'scenario_init_20261029')
  AND COALESCE(p.update_by, '') = ''
  AND (p.update_time IS NULL OR p.update_time = p.create_time)
  AND EXISTS (
      SELECT 1 FROM sys_policy_version v
      WHERE v.policy_id = p.id AND v.version_no = 1 AND v.change_type = 'CREATE'
        AND BINARY v.env_expr = BINARY s.old_env_expr AND v.condition_version = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_policy_version v WHERE v.policy_id = p.id AND v.version_no > 1
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_policy_bind b WHERE b.policy_id = p.id
  );
SELECT ROW_COUNT() AS refreshed_policies;

INSERT INTO sys_policy_version (
    policy_id, version_no, policy_code, policy_name, effect, subject_type, subject_expr,
    resource_expr, env_expr, condition_version, priority, status, remark,
    change_type, change_by, change_time
)
SELECT p.id, 2, p.policy_code, p.policy_name, p.effect, p.subject_type, p.subject_expr,
       p.resource_expr, p.env_expr, p.condition_version, p.priority, p.status, p.remark,
       'UPDATE', p.update_by, p.update_time
FROM sys_policy p
JOIN access_scenario_refresh s ON BINARY p.policy_code = BINARY s.policy_code
WHERE p.update_by = 'scenario_refresh_20261120'
  AND p.condition_version = 1 AND p.del_flag = '0'
  AND BINARY p.env_expr = BINARY s.env_expr
  AND NOT EXISTS (
      SELECT 1 FROM sys_policy_version v WHERE v.policy_id = p.id AND v.version_no > 1
  );
SELECT ROW_COUNT() AS added_versions;

COMMIT;
DROP TEMPORARY TABLE access_scenario_refresh;
