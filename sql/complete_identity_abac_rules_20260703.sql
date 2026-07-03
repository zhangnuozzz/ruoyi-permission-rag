-- ============================================================
-- VACP 第一轮完全实现：身份信息管理模块 + ABAC 强规则
-- 对齐原型文档：
-- 1. 用户属性：user_id / department / phone / employee_no / secret / User_groups / status / start_time / end_time
-- 2. 用户组属性：group_id / 负责人 / 绑定用户 / 用户组密级继承自组员中的最低密级
-- 3. 文档元数据：doc_id / upload_user_id / secret_level / doc_group_id / status
-- 4. ABAC：文档密级 <= 所绑定用户组密级；文档必须绑定唯一用户组；doc.status == ACTIVE
-- ============================================================

-- 1. 用户安全属性补齐：department / phone / employee_no
ALTER TABLE sys_user_security_attr
ADD COLUMN IF NOT EXISTS department VARCHAR(100) DEFAULT NULL COMMENT '所属部门，对应原型 department'
AFTER nick_name;

ALTER TABLE sys_user_security_attr
ADD COLUMN IF NOT EXISTS phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话'
AFTER department;

ALTER TABLE sys_user_security_attr
ADD COLUMN IF NOT EXISTS employee_no VARCHAR(64) DEFAULT NULL COMMENT '人员工号'
AFTER phone;

-- 回填 admin 示例数据
UPDATE sys_user_security_attr
SET department = COALESCE(department, '研发部门'),
    phone = COALESCE(phone, '15888888888'),
    employee_no = COALESCE(employee_no, CONCAT('EMP-', user_id))
WHERE user_id IS NOT NULL;

-- 2. 用户组属性补齐：负责人
ALTER TABLE sys_group
ADD COLUMN IF NOT EXISTS manager_user_id BIGINT DEFAULT NULL COMMENT '负责人用户ID'
AFTER group_name;

ALTER TABLE sys_group
ADD COLUMN IF NOT EXISTS manager_name VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名'
AFTER manager_user_id;

-- 回填负责人，演示环境默认 admin 负责
UPDATE sys_group
SET manager_user_id = COALESCE(manager_user_id, 1),
    manager_name = COALESCE(manager_name, 'admin')
WHERE del_flag = '0';

-- 3. 文档元数据补齐：doc_group_id
ALTER TABLE sys_rag_doc
ADD COLUMN IF NOT EXISTS doc_group_id VARCHAR(64) DEFAULT NULL COMMENT '文档用户组ID，对应原型 doc_group_id'
AFTER owner_group_code;

-- 用 owner_group_code 回填 doc_group_id
UPDATE sys_rag_doc
SET doc_group_id = owner_group_code
WHERE doc_group_id IS NULL OR doc_group_id = '';

-- 公开文档强制绑定公开组
UPDATE sys_rag_doc
SET owner_group_code = 'GROUP_PUBLIC',
    doc_group_id = 'GROUP_PUBLIC',
    owner_group_name = '公开文档组',
    owner_group_secret_level = 'PUBLIC',
    scope_code = 'PUBLIC'
WHERE security_level = 'PUBLIC';

-- 4. 确保公开组存在
INSERT INTO sys_group
(group_code, group_name, manager_user_id, manager_name, scope_code, group_secret_level, status, remark, create_by, create_time, del_flag)
VALUES
('GROUP_PUBLIC', '公开文档组', 1, 'admin', 'PUBLIC', 'PUBLIC', '0', '公开级文档默认用户组，对应原型 all', 'admin', NOW(), '0')
ON DUPLICATE KEY UPDATE
group_name = VALUES(group_name),
manager_user_id = VALUES(manager_user_id),
manager_name = VALUES(manager_name),
scope_code = VALUES(scope_code),
group_secret_level = VALUES(group_secret_level),
status = VALUES(status),
remark = VALUES(remark),
del_flag = VALUES(del_flag);

-- 5. 用户组密级自动计算过程：继承自组员中的最低密级
DROP PROCEDURE IF EXISTS refresh_group_secret_level;

DELIMITER //
CREATE PROCEDURE refresh_group_secret_level(IN p_group_id BIGINT)
BEGIN
    DECLARE v_min_rank INT DEFAULT NULL;
    DECLARE v_level VARCHAR(32) DEFAULT 'PUBLIC';

    SELECT MIN(
        CASE usa.secret_level
            WHEN 'PUBLIC' THEN 1
            WHEN 'INTERNAL' THEN 2
            WHEN 'SECRET' THEN 3
            WHEN 'CONFIDENTIAL' THEN 4
            ELSE 1
        END
    )
    INTO v_min_rank
    FROM sys_user_group_rel rel
    JOIN sys_user_security_attr usa ON usa.user_id = rel.user_id
    WHERE rel.group_id = p_group_id;

    IF v_min_rank IS NULL THEN
        SET v_level = 'PUBLIC';
    ELSEIF v_min_rank = 1 THEN
        SET v_level = 'PUBLIC';
    ELSEIF v_min_rank = 2 THEN
        SET v_level = 'INTERNAL';
    ELSEIF v_min_rank = 3 THEN
        SET v_level = 'SECRET';
    ELSE
        SET v_level = 'CONFIDENTIAL';
    END IF;

    UPDATE sys_group
    SET group_secret_level = v_level,
        update_by = 'system',
        update_time = NOW()
    WHERE id = p_group_id
      AND del_flag = '0';
END//
DELIMITER ;

-- 6. 批量刷新所有用户组密级
DROP PROCEDURE IF EXISTS refresh_all_group_secret_levels;

DELIMITER //
CREATE PROCEDURE refresh_all_group_secret_levels()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_group_id BIGINT;
    DECLARE cur CURSOR FOR SELECT id FROM sys_group WHERE del_flag = '0';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_group_id;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        CALL refresh_group_secret_level(v_group_id);
    END LOOP;

    CLOSE cur;

    -- 公开组必须保持 PUBLIC
    UPDATE sys_group
    SET group_secret_level = 'PUBLIC',
        scope_code = 'PUBLIC'
    WHERE group_code = 'GROUP_PUBLIC';
END//
DELIMITER ;

CALL refresh_all_group_secret_levels();

-- 7. 用户-用户组绑定变化时，自动刷新组密级
DROP TRIGGER IF EXISTS trg_user_group_rel_ai_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_group_rel_au_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_group_rel_ad_refresh_secret;

DELIMITER //
CREATE TRIGGER trg_user_group_rel_ai_refresh_secret
AFTER INSERT ON sys_user_group_rel
FOR EACH ROW
BEGIN
    CALL refresh_group_secret_level(NEW.group_id);
END//

CREATE TRIGGER trg_user_group_rel_au_refresh_secret
AFTER UPDATE ON sys_user_group_rel
FOR EACH ROW
BEGIN
    CALL refresh_group_secret_level(OLD.group_id);
    CALL refresh_group_secret_level(NEW.group_id);
END//

CREATE TRIGGER trg_user_group_rel_ad_refresh_secret
AFTER DELETE ON sys_user_group_rel
FOR EACH ROW
BEGIN
    CALL refresh_group_secret_level(OLD.group_id);
END//
DELIMITER ;

-- 8. 用户密级变化时，自动刷新其所属用户组密级
DROP TRIGGER IF EXISTS trg_user_security_attr_au_refresh_group_secret;

DELIMITER //
CREATE TRIGGER trg_user_security_attr_au_refresh_group_secret
AFTER UPDATE ON sys_user_security_attr
FOR EACH ROW
BEGIN
    IF OLD.secret_level <> NEW.secret_level THEN
        BEGIN
            DECLARE done INT DEFAULT 0;
            DECLARE v_group_id BIGINT;
            DECLARE cur CURSOR FOR SELECT group_id FROM sys_user_group_rel WHERE user_id = NEW.user_id;
            DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

            OPEN cur;

            read_loop: LOOP
                FETCH cur INTO v_group_id;
                IF done = 1 THEN
                    LEAVE read_loop;
                END IF;
                CALL refresh_group_secret_level(v_group_id);
            END LOOP;

            CLOSE cur;
        END;
    END IF;
END//
DELIMITER ;

-- 9. 文档 ABAC 强规则：INSERT
DROP TRIGGER IF EXISTS trg_rag_doc_bi_abac_validate;

DELIMITER //
CREATE TRIGGER trg_rag_doc_bi_abac_validate
BEFORE INSERT ON sys_rag_doc
FOR EACH ROW
BEGIN
    DECLARE v_group_name VARCHAR(128);
    DECLARE v_group_level VARCHAR(32);
    DECLARE v_doc_rank INT DEFAULT 1;
    DECLARE v_group_rank INT DEFAULT 1;

    IF NEW.metadata_status IS NULL OR NEW.metadata_status = '' THEN
        SET NEW.metadata_status = 'ACTIVE';
    END IF;

    IF NEW.status IS NULL OR NEW.status = '' THEN
        SET NEW.status = '0';
    END IF;

    IF NEW.security_level = 'PUBLIC' THEN
        SET NEW.owner_group_code = 'GROUP_PUBLIC';
        SET NEW.doc_group_id = 'GROUP_PUBLIC';
        SET NEW.owner_group_name = '公开文档组';
        SET NEW.owner_group_secret_level = 'PUBLIC';
        SET NEW.scope_code = 'PUBLIC';
    END IF;

    IF NEW.owner_group_code IS NULL OR NEW.owner_group_code = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VACP_ABAC_DENY: 文档必须绑定唯一用户组 doc_group_id';
    END IF;

    SET NEW.doc_group_id = NEW.owner_group_code;

    SELECT group_name, group_secret_level
    INTO v_group_name, v_group_level
    FROM sys_group
    WHERE group_code = NEW.owner_group_code
      AND status = '0'
      AND del_flag = '0'
    LIMIT 1;

    IF v_group_level IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VACP_ABAC_DENY: 文档绑定的用户组不存在或已停用';
    END IF;

    SET NEW.owner_group_name = v_group_name;
    SET NEW.owner_group_secret_level = v_group_level;

    SET v_doc_rank =
        CASE NEW.security_level
            WHEN 'PUBLIC' THEN 1
            WHEN 'INTERNAL' THEN 2
            WHEN 'SECRET' THEN 3
            WHEN 'CONFIDENTIAL' THEN 4
            ELSE 1
        END;

    SET v_group_rank =
        CASE v_group_level
            WHEN 'PUBLIC' THEN 1
            WHEN 'INTERNAL' THEN 2
            WHEN 'SECRET' THEN 3
            WHEN 'CONFIDENTIAL' THEN 4
            ELSE 1
        END;

    IF v_doc_rank > v_group_rank THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VACP_ABAC_DENY: 文档密级不能高于所绑定用户组密级';
    END IF;
END//
DELIMITER ;

-- 10. 文档 ABAC 强规则：UPDATE
DROP TRIGGER IF EXISTS trg_rag_doc_bu_abac_validate;

DELIMITER //
CREATE TRIGGER trg_rag_doc_bu_abac_validate
BEFORE UPDATE ON sys_rag_doc
FOR EACH ROW
BEGIN
    DECLARE v_group_name VARCHAR(128);
    DECLARE v_group_level VARCHAR(32);
    DECLARE v_doc_rank INT DEFAULT 1;
    DECLARE v_group_rank INT DEFAULT 1;

    IF NEW.metadata_status IS NULL OR NEW.metadata_status = '' THEN
        SET NEW.metadata_status = 'ACTIVE';
    END IF;

    IF NEW.status IS NULL OR NEW.status = '' THEN
        SET NEW.status = '0';
    END IF;

    IF NEW.security_level = 'PUBLIC' THEN
        SET NEW.owner_group_code = 'GROUP_PUBLIC';
        SET NEW.doc_group_id = 'GROUP_PUBLIC';
        SET NEW.owner_group_name = '公开文档组';
        SET NEW.owner_group_secret_level = 'PUBLIC';
        SET NEW.scope_code = 'PUBLIC';
    END IF;

    IF NEW.owner_group_code IS NULL OR NEW.owner_group_code = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VACP_ABAC_DENY: 文档必须绑定唯一用户组 doc_group_id';
    END IF;

    SET NEW.doc_group_id = NEW.owner_group_code;

    SELECT group_name, group_secret_level
    INTO v_group_name, v_group_level
    FROM sys_group
    WHERE group_code = NEW.owner_group_code
      AND status = '0'
      AND del_flag = '0'
    LIMIT 1;

    IF v_group_level IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VACP_ABAC_DENY: 文档绑定的用户组不存在或已停用';
    END IF;

    SET NEW.owner_group_name = v_group_name;
    SET NEW.owner_group_secret_level = v_group_level;

    SET v_doc_rank =
        CASE NEW.security_level
            WHEN 'PUBLIC' THEN 1
            WHEN 'INTERNAL' THEN 2
            WHEN 'SECRET' THEN 3
            WHEN 'CONFIDENTIAL' THEN 4
            ELSE 1
        END;

    SET v_group_rank =
        CASE v_group_level
            WHEN 'PUBLIC' THEN 1
            WHEN 'INTERNAL' THEN 2
            WHEN 'SECRET' THEN 3
            WHEN 'CONFIDENTIAL' THEN 4
            ELSE 1
        END;

    IF v_doc_rank > v_group_rank THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VACP_ABAC_DENY: 文档密级不能高于所绑定用户组密级';
    END IF;
END//
DELIMITER ;

-- 11. 同步现有文档 doc_group_id 与公开文档规则
UPDATE sys_rag_doc d
LEFT JOIN sys_group g ON g.group_code = d.owner_group_code
SET d.doc_group_id = d.owner_group_code,
    d.owner_group_name = g.group_name,
    d.owner_group_secret_level = g.group_secret_level
WHERE d.del_flag = '0'
  AND d.owner_group_code IS NOT NULL
  AND d.owner_group_code <> '';

UPDATE sys_rag_doc
SET owner_group_code = 'GROUP_PUBLIC',
    doc_group_id = 'GROUP_PUBLIC',
    owner_group_name = '公开文档组',
    owner_group_secret_level = 'PUBLIC',
    scope_code = 'PUBLIC'
WHERE security_level = 'PUBLIC';

-- 12. RAG 文件表同步公开文档 all/GROUP_PUBLIC 规则
UPDATE sys_rag_file
SET group_id = 'GROUP_PUBLIC',
    group_name = '公开文档组',
    scope_code = 'PUBLIC'
WHERE security_level = 'PUBLIC';
