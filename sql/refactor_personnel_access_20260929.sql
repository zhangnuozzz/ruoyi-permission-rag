USE `ry-vue-320`;

-- ============================================================
-- 向量库访问控制系统
-- 人员权限管理第一阶段重构
--
-- 人员 = 基础身份 + 组织归属 + 安全密级 + 知悉用户组
--
-- 知悉范围：
-- 用户所属用户组集合
--
-- PUBLIC：
-- 公开非密数据
--
-- INTERNAL：
-- 内部工作秘密
--
-- SECRET：
-- 国家秘密·秘密级
--
-- CONFIDENTIAL：
-- 国家秘密·机密级
-- ============================================================


-- ------------------------------------------------------------
-- 1. sys_user 增加人员信息
-- ------------------------------------------------------------

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS ethnicity VARCHAR(64) DEFAULT NULL
COMMENT '民族'
AFTER sex;

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS id_card_no VARCHAR(64) DEFAULT NULL
COMMENT '身份证号码'
AFTER ethnicity;

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS birth_place VARCHAR(255) DEFAULT NULL
COMMENT '出生地'
AFTER id_card_no;

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS job_title VARCHAR(128) DEFAULT NULL
COMMENT '职务'
AFTER birth_place;

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS employee_no VARCHAR(64) DEFAULT NULL
COMMENT '人员工号'
AFTER job_title;


-- ------------------------------------------------------------
-- 2. 身份权限中心菜单重构
-- ------------------------------------------------------------

-- 原“用户权限配置”改成“人员权限管理”
-- 页面直接复用并改造若依原用户管理页面
UPDATE sys_menu
SET
    menu_name = '人员权限管理',
    path = 'personnel',
    component = 'system/user/index',
    perms = 'system:user:list',
    parent_id = 2061,
    order_num = 1,
    visible = '0',
    status = '0'
WHERE menu_id = 2050;


-- 隐藏系统管理下面原生的重复“用户管理”
-- 只隐藏菜单，不删除底层接口
UPDATE sys_menu
SET visible = '1'
WHERE component = 'system/user/index'
  AND menu_id <> 2050;


-- 身份权限中心其他页面顺序
UPDATE sys_menu
SET order_num = 2
WHERE menu_id = 2000;

UPDATE sys_menu
SET order_num = 3
WHERE menu_id = 2006;

UPDATE sys_menu
SET order_num = 4
WHERE menu_id = 2044;


-- 策略绑定继续隐藏
UPDATE sys_menu
SET
    visible = '1',
    order_num = 99
WHERE menu_id = 2012;


-- ------------------------------------------------------------
-- 3. 查看结果
-- ------------------------------------------------------------

SELECT
    menu_id,
    menu_name,
    parent_id,
    order_num,
    path,
    component,
    visible,
    status
FROM sys_menu
WHERE parent_id = 2061
ORDER BY order_num;

