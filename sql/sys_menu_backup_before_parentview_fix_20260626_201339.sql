/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.7.2-MariaDB, for osx10.20 (arm64)
--
-- Host: localhost    Database: ry-vue-320
-- ------------------------------------------------------
-- Server version	11.7.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `is_frame` int(1) DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int(1) DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2046 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES
(1,'系统管理',0,1,'system',NULL,1,0,'M','0','0','','system','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','系统管理目录'),
(2,'系统监控',0,2,'monitor',NULL,1,0,'M','1','0','','monitor','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','系统监控目录'),
(3,'系统工具',0,3,'tool',NULL,1,0,'M','1','0','','tool','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','系统工具目录'),
(4,'若依官网',0,4,'http://ruoyi.vip',NULL,0,0,'M','1','0','','guide','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','若依官网地址'),
(100,'用户管理',1,1,'user','system/user/index',1,0,'C','0','0','system:user:list','user','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','用户管理菜单'),
(101,'角色管理',1,2,'role','system/role/index',1,0,'C','0','0','system:role:list','peoples','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','角色管理菜单'),
(102,'菜单管理',1,3,'menu','system/menu/index',1,0,'C','0','0','system:menu:list','tree-table','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','菜单管理菜单'),
(103,'部门管理',1,4,'dept','system/dept/index',1,0,'C','0','0','system:dept:list','tree','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','部门管理菜单'),
(104,'岗位管理',1,5,'post','system/post/index',1,0,'C','0','0','system:post:list','post','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','岗位管理菜单'),
(105,'字典管理',1,6,'dict','system/dict/index',1,0,'C','0','0','system:dict:list','dict','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','字典管理菜单'),
(106,'参数设置',1,7,'config','system/config/index',1,0,'C','0','0','system:config:list','edit','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','参数设置菜单'),
(107,'通知公告',1,8,'notice','system/notice/index',1,0,'C','0','0','system:notice:list','message','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','通知公告菜单'),
(108,'日志管理',1,9,'log','system/log/index',1,0,'M','0','0','','log','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','日志管理菜单'),
(109,'在线用户',2,1,'online','monitor/online/index',1,0,'C','1','0','monitor:online:list','online','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','在线用户菜单'),
(110,'定时任务',2,2,'job','monitor/job/index',1,0,'C','1','0','monitor:job:list','job','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','定时任务菜单'),
(111,'数据监控',2,3,'druid','monitor/druid/index',1,0,'C','1','0','monitor:druid:list','druid','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','数据监控菜单'),
(112,'服务监控',2,4,'server','monitor/server/index',1,0,'C','1','0','monitor:server:list','server','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','服务监控菜单'),
(113,'表单构建',3,1,'build','tool/build/index',1,0,'C','1','0','tool:build:list','build','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','表单构建菜单'),
(114,'代码生成',3,2,'gen','tool/gen/index',1,0,'C','1','0','tool:gen:list','code','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','代码生成菜单'),
(115,'系统接口',3,3,'swagger','tool/swagger/index',1,0,'C','1','0','tool:swagger:list','swagger','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','系统接口菜单'),
(500,'操作日志',108,1,'operlog','monitor/operlog/index',1,0,'C','0','0','monitor:operlog:list','form','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','操作日志菜单'),
(501,'登录日志',108,2,'logininfor','monitor/logininfor/index',1,0,'C','0','0','monitor:logininfor:list','logininfor','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00','登录日志菜单'),
(1001,'用户查询',100,1,'','',1,0,'F','0','0','system:user:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1002,'用户新增',100,2,'','',1,0,'F','0','0','system:user:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1003,'用户修改',100,3,'','',1,0,'F','0','0','system:user:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1004,'用户删除',100,4,'','',1,0,'F','0','0','system:user:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1005,'用户导出',100,5,'','',1,0,'F','0','0','system:user:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1006,'用户导入',100,6,'','',1,0,'F','0','0','system:user:import','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1007,'重置密码',100,7,'','',1,0,'F','0','0','system:user:resetPwd','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1008,'角色查询',101,1,'','',1,0,'F','0','0','system:role:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1009,'角色新增',101,2,'','',1,0,'F','0','0','system:role:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1010,'角色修改',101,3,'','',1,0,'F','0','0','system:role:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1011,'角色删除',101,4,'','',1,0,'F','0','0','system:role:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1012,'角色导出',101,5,'','',1,0,'F','0','0','system:role:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1013,'菜单查询',102,1,'','',1,0,'F','0','0','system:menu:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1014,'菜单新增',102,2,'','',1,0,'F','0','0','system:menu:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1015,'菜单修改',102,3,'','',1,0,'F','0','0','system:menu:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1016,'菜单删除',102,4,'','',1,0,'F','0','0','system:menu:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1017,'部门查询',103,1,'','',1,0,'F','0','0','system:dept:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1018,'部门新增',103,2,'','',1,0,'F','0','0','system:dept:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1019,'部门修改',103,3,'','',1,0,'F','0','0','system:dept:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1020,'部门删除',103,4,'','',1,0,'F','0','0','system:dept:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1021,'岗位查询',104,1,'','',1,0,'F','0','0','system:post:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1022,'岗位新增',104,2,'','',1,0,'F','0','0','system:post:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1023,'岗位修改',104,3,'','',1,0,'F','0','0','system:post:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1024,'岗位删除',104,4,'','',1,0,'F','0','0','system:post:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1025,'岗位导出',104,5,'','',1,0,'F','0','0','system:post:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1026,'字典查询',105,1,'#','',1,0,'F','0','0','system:dict:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1027,'字典新增',105,2,'#','',1,0,'F','0','0','system:dict:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1028,'字典修改',105,3,'#','',1,0,'F','0','0','system:dict:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1029,'字典删除',105,4,'#','',1,0,'F','0','0','system:dict:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1030,'字典导出',105,5,'#','',1,0,'F','0','0','system:dict:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1031,'参数查询',106,1,'#','',1,0,'F','0','0','system:config:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1032,'参数新增',106,2,'#','',1,0,'F','0','0','system:config:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1033,'参数修改',106,3,'#','',1,0,'F','0','0','system:config:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1034,'参数删除',106,4,'#','',1,0,'F','0','0','system:config:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1035,'参数导出',106,5,'#','',1,0,'F','0','0','system:config:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1036,'公告查询',107,1,'#','',1,0,'F','0','0','system:notice:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1037,'公告新增',107,2,'#','',1,0,'F','0','0','system:notice:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1038,'公告修改',107,3,'#','',1,0,'F','0','0','system:notice:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1039,'公告删除',107,4,'#','',1,0,'F','0','0','system:notice:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1040,'操作查询',500,1,'#','',1,0,'F','0','0','monitor:operlog:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1041,'操作删除',500,2,'#','',1,0,'F','0','0','monitor:operlog:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1042,'日志导出',500,4,'#','',1,0,'F','0','0','monitor:operlog:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1043,'登录查询',501,1,'#','',1,0,'F','0','0','monitor:logininfor:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1044,'登录删除',501,2,'#','',1,0,'F','0','0','monitor:logininfor:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1045,'日志导出',501,3,'#','',1,0,'F','0','0','monitor:logininfor:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1046,'在线查询',109,1,'#','',1,0,'F','0','0','monitor:online:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1047,'批量强退',109,2,'#','',1,0,'F','0','0','monitor:online:batchLogout','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1048,'单条强退',109,3,'#','',1,0,'F','0','0','monitor:online:forceLogout','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1049,'任务查询',110,1,'#','',1,0,'F','0','0','monitor:job:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1050,'任务新增',110,2,'#','',1,0,'F','0','0','monitor:job:add','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1051,'任务修改',110,3,'#','',1,0,'F','0','0','monitor:job:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1052,'任务删除',110,4,'#','',1,0,'F','0','0','monitor:job:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1053,'状态修改',110,5,'#','',1,0,'F','0','0','monitor:job:changeStatus','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1054,'任务导出',110,7,'#','',1,0,'F','0','0','monitor:job:export','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1055,'生成查询',114,1,'#','',1,0,'F','0','0','tool:gen:query','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1056,'生成修改',114,2,'#','',1,0,'F','0','0','tool:gen:edit','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1057,'生成删除',114,3,'#','',1,0,'F','0','0','tool:gen:remove','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1058,'导入代码',114,2,'#','',1,0,'F','0','0','tool:gen:import','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1059,'预览代码',114,4,'#','',1,0,'F','0','0','tool:gen:preview','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(1060,'生成代码',114,5,'#','',1,0,'F','0','0','tool:gen:code','#','admin','2018-03-16 11:33:00','ry','2018-03-16 11:33:00',''),
(2000,'用户组',1,2,'group','system/group/index',1,0,'C','0','0','system:group:list','#','admin','2026-05-05 21:27:37','',NULL,'用户组菜单'),
(2001,'用户组查询',2000,1,'#','',1,0,'F','0','0','system:group:query','#','admin','2026-05-05 21:27:37','',NULL,''),
(2002,'用户组新增',2000,2,'#','',1,0,'F','0','0','system:group:add','#','admin','2026-05-05 21:27:37','',NULL,''),
(2003,'用户组修改',2000,3,'#','',1,0,'F','0','0','system:group:edit','#','admin','2026-05-05 21:27:37','',NULL,''),
(2004,'用户组删除',2000,4,'#','',1,0,'F','0','0','system:group:remove','#','admin','2026-05-05 21:27:37','',NULL,''),
(2005,'用户组导出',2000,5,'#','',1,0,'F','0','0','system:group:export','#','admin','2026-05-05 21:27:37','',NULL,''),
(2006,'权限策略定义',1,3,'policy','system/policy/index',1,0,'C','0','0','system:policy:list','#','admin','2026-05-05 21:39:20','',NULL,'权限策略定义菜单'),
(2007,'权限策略定义查询',2006,1,'#','',1,0,'F','0','0','system:policy:query','#','admin','2026-05-05 21:39:20','',NULL,''),
(2008,'权限策略定义新增',2006,2,'#','',1,0,'F','0','0','system:policy:add','#','admin','2026-05-05 21:39:20','',NULL,''),
(2009,'权限策略定义修改',2006,3,'#','',1,0,'F','0','0','system:policy:edit','#','admin','2026-05-05 21:39:20','',NULL,''),
(2010,'权限策略定义删除',2006,4,'#','',1,0,'F','0','0','system:policy:remove','#','admin','2026-05-05 21:39:20','',NULL,''),
(2011,'权限策略定义导出',2006,5,'#','',1,0,'F','0','0','system:policy:export','#','admin','2026-05-05 21:39:20','',NULL,''),
(2012,'策略绑定管理',1,4,'policyBind','system/policyBind/index',1,0,'C','0','0','system:policyBind:list','#','admin','2026-05-05 21:47:40','',NULL,'策略绑定管理菜单'),
(2013,'策略绑定管理查询',2012,1,'#','',1,0,'F','0','0','system:policyBind:query','#','admin','2026-05-05 21:47:40','',NULL,''),
(2014,'策略绑定管理新增',2012,2,'#','',1,0,'F','0','0','system:policyBind:add','#','admin','2026-05-05 21:47:40','',NULL,''),
(2015,'策略绑定管理修改',2012,3,'#','',1,0,'F','0','0','system:policyBind:edit','#','admin','2026-05-05 21:47:40','',NULL,''),
(2016,'策略绑定管理删除',2012,4,'#','',1,0,'F','0','0','system:policyBind:remove','#','admin','2026-05-05 21:47:40','',NULL,''),
(2017,'策略绑定管理导出',2012,5,'#','',1,0,'F','0','0','system:policyBind:export','#','admin','2026-05-05 21:47:40','',NULL,''),
(2018,'RAG审计日志',1,7,'ragAuditLog','rag/auditLog/index',1,0,'C','0','0','rag:audit:list','#','admin','2026-05-05 22:27:04','',NULL,'RAG检索审计日志菜单'),
(2019,'RAG检索审计日志查询',2018,1,'#','',1,0,'F','0','0','system:log:query','#','admin','2026-05-05 22:27:04','',NULL,''),
(2020,'RAG检索审计日志新增',2018,2,'#','',1,0,'F','0','0','system:log:add','#','admin','2026-05-05 22:27:04','',NULL,''),
(2021,'RAG检索审计日志修改',2018,3,'#','',1,0,'F','0','0','system:log:edit','#','admin','2026-05-05 22:27:04','',NULL,''),
(2022,'RAG检索审计日志删除',2018,4,'#','',1,0,'F','0','0','system:log:remove','#','admin','2026-05-05 22:27:04','',NULL,''),
(2023,'RAG检索审计日志导出',2018,5,'#','',1,0,'F','0','0','system:log:export','#','admin','2026-05-05 22:27:04','',NULL,''),
(2024,'文档权限标签',1,5,'ragDoc','system/ragDoc/index',1,0,'C','0','0','system:ragDoc:list','#','admin','2026-05-06 18:51:19','',NULL,'文档权限标签菜单'),
(2025,'文档权限标签查询',2024,1,'#','',1,0,'F','0','0','system:ragDoc:query','#','admin','2026-05-06 18:51:19','',NULL,''),
(2026,'文档权限标签新增',2024,2,'#','',1,0,'F','0','0','system:ragDoc:add','#','admin','2026-05-06 18:51:19','',NULL,''),
(2027,'文档权限标签修改',2024,3,'#','',1,0,'F','0','0','system:ragDoc:edit','#','admin','2026-05-06 18:51:19','',NULL,''),
(2028,'文档权限标签删除',2024,4,'#','',1,0,'F','0','0','system:ragDoc:remove','#','admin','2026-05-06 18:51:19','',NULL,''),
(2029,'文档权限标签导出',2024,5,'#','',1,0,'F','0','0','system:ragDoc:export','#','admin','2026-05-06 18:51:19','',NULL,''),
(2030,'RAG检索测试',1,6,'ragSearch','rag/search/index',1,0,'C','0','0','rag:search:test','search','admin','2026-05-06 18:57:00','',NULL,'RAG安全检索测试页面'),
(2031,'安全中心',0,8,'security',NULL,1,0,'M','0','0','','lock','admin','2026-05-19 08:45:51','',NULL,'安全中心目录'),
(2032,'访问监控',2031,1,'accessLog','system/accessLog/index',1,0,'C','0','0','system:accessLog:list','monitor','admin','2026-05-19 08:45:51','',NULL,'访问监控菜单'),
(2033,'访问监控查询',2032,1,'#','',1,0,'F','0','0','system:accessLog:query','#','admin','2026-05-19 08:45:51','',NULL,''),
(2034,'访问监控删除',2032,2,'#','',1,0,'F','0','0','system:accessLog:remove','#','admin','2026-05-19 08:45:51','',NULL,''),
(2035,'访问监控导出',2032,3,'#','',1,0,'F','0','0','system:accessLog:export','#','admin','2026-05-19 08:45:51','',NULL,''),
(2036,'IP黑名单',2031,2,'ipBlacklist','system/ipBlacklist/index',1,0,'C','0','0','system:ipBlacklist:list','lock','admin','2026-05-19 09:03:06','',NULL,'IP黑名单菜单'),
(2037,'IP黑名单查询',2036,1,'#','',1,0,'F','0','0','system:ipBlacklist:query','#','admin','2026-05-19 09:03:06','',NULL,''),
(2038,'IP黑名单新增',2036,2,'#','',1,0,'F','0','0','system:ipBlacklist:add','#','admin','2026-05-19 09:03:06','',NULL,''),
(2039,'IP黑名单修改',2036,3,'#','',1,0,'F','0','0','system:ipBlacklist:edit','#','admin','2026-05-19 09:03:06','',NULL,''),
(2040,'IP黑名单删除',2036,4,'#','',1,0,'F','0','0','system:ipBlacklist:remove','#','admin','2026-05-19 09:03:06','',NULL,''),
(2041,'RAG文件入库',1,8,'ragFile','rag/file/index',1,0,'C','0','0','rag:file:list','upload','admin','2026-05-19 09:47:37','',NULL,'RAG文件入库页面'),
(2042,'RAG文件查询',2041,1,'#','',1,0,'F','0','0','rag:file:list','#','admin','2026-05-19 09:47:37','',NULL,''),
(2043,'RAG文件上传',2041,2,'#','',1,0,'F','0','0','rag:file:upload','#','admin','2026-05-19 09:47:37','',NULL,''),
(2044,'RAG权限上下文',1,9,'permissionContext','rag/permissionContext/index',1,0,'C','0','0','rag:permissionContext:view','tree','admin','2026-05-21 09:22:52','',NULL,'RAG权限上下文预览页面'),
(2045,'RAG权限上下文查看',2044,1,'#','',1,0,'F','0','0','rag:permissionContext:view','#','admin','2026-05-21 09:22:52','',NULL,'');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-06-26 20:13:44
