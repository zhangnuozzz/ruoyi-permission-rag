USE `ry-vue-320`;

-- ============================================================
-- 验收演示组织机构
--
-- 只改部门名称，不改变 dept_id、parent_id、ancestors。
-- 因此已有用户的 dept_id 关系不会被破坏。
-- ============================================================

UPDATE sys_dept
SET dept_name = '某科研单位'
WHERE dept_id = 100;

UPDATE sys_dept
SET dept_name = '信息技术中心'
WHERE dept_id = 101;

UPDATE sys_dept
SET dept_name = '综合管理中心'
WHERE dept_id = 102;

UPDATE sys_dept
SET dept_name = '研发部'
WHERE dept_id = 103;

UPDATE sys_dept
SET dept_name = '数据资源部'
WHERE dept_id = 104;

UPDATE sys_dept
SET dept_name = '测试部'
WHERE dept_id = 105;

UPDATE sys_dept
SET dept_name = '安全管理部'
WHERE dept_id = 106;

UPDATE sys_dept
SET dept_name = '运维保障部'
WHERE dept_id = 107;

UPDATE sys_dept
SET dept_name = '综合办公室'
WHERE dept_id = 108;

UPDATE sys_dept
SET dept_name = '人事管理部'
WHERE dept_id = 109;

UPDATE sys_dept
SET dept_name = '财务部'
WHERE dept_id = 110;

SELECT
    dept_id,
    parent_id,
    dept_name,
    order_num,
    status
FROM sys_dept
ORDER BY dept_id;
