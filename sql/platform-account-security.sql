-- 仅新增凭据管理记录；既有账号从本次升级时间开始计算有效期。
CREATE TABLE IF NOT EXISTS sys_password_state (
    user_id BIGINT NOT NULL PRIMARY KEY,
    changed_at DATETIME(3) NOT NULL,
    change_required TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sys_password_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    changed_at DATETIME(3) NOT NULL,
    KEY idx_password_history_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
START TRANSACTION;
INSERT INTO sys_password_history (user_id, password_hash, changed_at)
SELECT u.user_id, u.password, NOW(3) FROM sys_user u
WHERE u.del_flag = '0' AND u.password IS NOT NULL AND u.password <> ''
AND NOT EXISTS (SELECT 1 FROM sys_password_state s WHERE s.user_id = u.user_id)
AND NOT EXISTS (SELECT 1 FROM sys_password_history h WHERE h.user_id = u.user_id AND BINARY h.password_hash = BINARY u.password);
INSERT INTO sys_password_state (user_id, changed_at, change_required)
SELECT u.user_id, NOW(3), 0 FROM sys_user u WHERE u.del_flag = '0'
AND NOT EXISTS (SELECT 1 FROM sys_password_state s WHERE s.user_id = u.user_id);
COMMIT;
