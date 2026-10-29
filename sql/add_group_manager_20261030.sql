-- MariaDB 10.11; nullable responsibility metadata, no membership or document authorization changes.
ALTER TABLE sys_group ADD COLUMN IF NOT EXISTS manager_user_id BIGINT NULL AFTER group_name;
