USE `ry-vue-320`;

-- ============================================================
-- 向量库访问控制系统
-- 核心权限模型纠正
--
-- 1. security_level = 真正的人员/文档密级
-- 2. sys_user_group_rel = 用户知悉范围集合
-- 3. owner_group_code = 国家秘密文档唯一知悉用户组
-- 4. scope_code 退出核心鉴权
-- 5. group_secret_level 退出核心鉴权
-- 6. 策略只能追加限制，不能授予国家秘密知悉权限
-- ============================================================


-- ------------------------------------------------------------
-- 一、停用旧“用户组最低密级自动计算”机制
-- ------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_user_group_rel_ai_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_group_rel_au_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_group_rel_ad_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_security_attr_au_refresh_group_secret;

DROP PROCEDURE IF EXISTS refresh_group_secret_level;
DROP PROCEDURE IF EXISTS refresh_all_group_secret_levels;


-- ------------------------------------------------------------
-- 二、删除旧文档 ABAC trigger
-- ------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_rag_doc_bi_abac_validate;
DROP TRIGGER IF EXISTS trg_rag_doc_bu_abac_validate;


-- ------------------------------------------------------------
-- 三、重新定义字段说明
-- ------------------------------------------------------------

ALTER TABLE sys_group
MODIFY COLUMN scope_code VARCHAR(64) DEFAULT NULL
COMMENT '兼容字段，不参与核心权限判定；知悉范围以用户组成员关系为准';

ALTER TABLE sys_group
MODIFY COLUMN group_secret_level VARCHAR(32) DEFAULT NULL
COMMENT '兼容字段，不参与核心权限判定';

ALTER TABLE sys_rag_doc
MODIFY COLUMN scope_code VARCHAR(64) DEFAULT NULL
COMMENT '兼容字段；SECRET/CONFIDENTIAL 时可与唯一用户组编码保持一致';


-- ------------------------------------------------------------
-- 四、现有数据规范化
-- ------------------------------------------------------------

-- PUBLIC：公开非密，GROUP_PUBLIC 只作兼容元数据
UPDATE sys_rag_doc
SET
    scope_code = 'PUBLIC',
    owner_group_code = 'GROUP_PUBLIC',
    doc_group_id = 'GROUP_PUBLIC',
    owner_group_name = '公开文档组'
WHERE security_level = 'PUBLIC'
  AND del_flag = '0';


-- INTERNAL：工作秘密，不依赖知悉用户组
UPDATE sys_rag_doc
SET
    scope_code = 'INTERNAL',
    owner_group_code = NULL,
    doc_group_id = NULL,
    owner_group_name = NULL,
    owner_group_secret_level = NULL
WHERE security_level = 'INTERNAL'
  AND del_flag = '0';


-- SECRET / CONFIDENTIAL：
-- scope_code 仅兼容展示，直接与唯一用户组编码一致
UPDATE sys_rag_doc
SET scope_code = owner_group_code
WHERE security_level IN ('SECRET', 'CONFIDENTIAL')
  AND owner_group_code IS NOT NULL
  AND owner_group_code <> ''
  AND del_flag = '0';


-- ------------------------------------------------------------
-- 五、重新建立 INSERT 强规则
-- ------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_rag_doc_bi_access_validate
BEFORE INSERT ON sys_rag_doc
FOR EACH ROW
BEGIN
    DECLARE v_group_name VARCHAR(128);
    DECLARE v_group_count INT DEFAULT 0;

    IF NEW.metadata_status IS NULL OR NEW.metadata_status = '' THEN
        SET NEW.metadata_status = 'ACTIVE';
    END IF;

    IF NEW.status IS NULL OR NEW.status = '' THEN
        SET NEW.status = '0';
    END IF;

    -- PUBLIC：公开数据
    IF NEW.security_level = 'PUBLIC' THEN

        SET NEW.scope_code = 'PUBLIC';

        -- GROUP_PUBLIC 仅作兼容，不参与鉴权
        SET NEW.owner_group_code = 'GROUP_PUBLIC';
        SET NEW.doc_group_id = 'GROUP_PUBLIC';
        SET NEW.owner_group_name = '公开文档组';
        SET NEW.owner_group_secret_level = NULL;

    -- INTERNAL：工作秘密，只依赖人员密级
    ELSEIF NEW.security_level = 'INTERNAL' THEN

        SET NEW.scope_code = 'INTERNAL';
        SET NEW.owner_group_code = NULL;
        SET NEW.doc_group_id = NULL;
        SET NEW.owner_group_name = NULL;
        SET NEW.owner_group_secret_level = NULL;

    -- SECRET / CONFIDENTIAL：必须绑定唯一知悉用户组
    ELSEIF NEW.security_level IN ('SECRET', 'CONFIDENTIAL') THEN

        IF NEW.owner_group_code IS NULL
           OR NEW.owner_group_code = '' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'ACCESS_DENY: 国家秘密文档必须绑定唯一知悉用户组';
        END IF;

        SELECT COUNT(1), MAX(group_name)
        INTO v_group_count, v_group_name
        FROM sys_group
        WHERE group_code = NEW.owner_group_code
          AND status = '0'
          AND del_flag = '0';

        IF v_group_count = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'ACCESS_DENY: 文档绑定用户组不存在或已停用';
        END IF;

        SET NEW.doc_group_id = NEW.owner_group_code;
        SET NEW.owner_group_name = v_group_name;

        -- 不再维护独立 scope 映射
        SET NEW.scope_code = NEW.owner_group_code;

        -- 用户组密级退出核心权限模型
        SET NEW.owner_group_secret_level = NULL;

    ELSE

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'ACCESS_DENY: 非法文档密级';
    END IF;
END//

DELIMITER ;


-- ------------------------------------------------------------
-- 六、重新建立 UPDATE 强规则
-- ------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_rag_doc_bu_access_validate
BEFORE UPDATE ON sys_rag_doc
FOR EACH ROW
BEGIN
    DECLARE v_group_name VARCHAR(128);
    DECLARE v_group_count INT DEFAULT 0;

    IF NEW.security_level = 'PUBLIC' THEN

        SET NEW.scope_code = 'PUBLIC';
        SET NEW.owner_group_code = 'GROUP_PUBLIC';
        SET NEW.doc_group_id = 'GROUP_PUBLIC';
        SET NEW.owner_group_name = '公开文档组';
        SET NEW.owner_group_secret_level = NULL;

    ELSEIF NEW.security_level = 'INTERNAL' THEN

        SET NEW.scope_code = 'INTERNAL';
        SET NEW.owner_group_code = NULL;
        SET NEW.doc_group_id = NULL;
        SET NEW.owner_group_name = NULL;
        SET NEW.owner_group_secret_level = NULL;

    ELSEIF NEW.security_level IN ('SECRET', 'CONFIDENTIAL') THEN

        IF NEW.owner_group_code IS NULL
           OR NEW.owner_group_code = '' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'ACCESS_DENY: 国家秘密文档必须绑定唯一知悉用户组';
        END IF;

        SELECT COUNT(1), MAX(group_name)
        INTO v_group_count, v_group_name
        FROM sys_group
        WHERE group_code = NEW.owner_group_code
          AND status = '0'
          AND del_flag = '0';

        IF v_group_count = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'ACCESS_DENY: 文档绑定用户组不存在或已停用';
        END IF;

        SET NEW.doc_group_id = NEW.owner_group_code;
        SET NEW.owner_group_name = v_group_name;
        SET NEW.scope_code = NEW.owner_group_code;
        SET NEW.owner_group_secret_level = NULL;

    ELSE

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'ACCESS_DENY: 非法文档密级';
    END IF;
END//

DELIMITER ;


-- ------------------------------------------------------------
-- 七、检查现有数据
-- ------------------------------------------------------------

SELECT
    doc_id,
    doc_name,
    security_level,
    owner_group_code,
    scope_code
FROM sys_rag_doc
WHERE del_flag = '0'
ORDER BY id;

