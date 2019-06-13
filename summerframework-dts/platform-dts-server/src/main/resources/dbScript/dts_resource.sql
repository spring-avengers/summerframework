DROP TABLE IF EXISTS `dts_branch_info`;

CREATE TABLE `dts_branch_info` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `trans_id` bigint(20) NOT NULL COMMENT '事务号',
  `branch_id` bigint(20) NOT NULL COMMENT '分支事务号',
  `log_info` longblob COMMENT 'undo/redo log',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int(10) DEFAULT NULL COMMENT '事务状态',
  `instance_id` varchar(250) DEFAULT NULL COMMENT '服务实例Id',
  PRIMARY KEY (`id`),
   UNIQUE KEY `idx_branch_id_trans_id` (`branch_id`,`trans_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='redo/undo备份日志表';


DROP TABLE IF EXISTS `dts_row_lock`;

CREATE TABLE `dts_row_lock` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL COMMENT '分支事务号',
  `trans_id` bigint(20) NOT NULL COMMENT '主事务号',
  `table_name` varchar(64) NOT NULL COMMENT '表名称',
  `row_key` varchar(250) NOT NULL COMMENT '行唯一key',
  `instance_id` varchar(250) NOT NULL COMMENT '实例id',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
PRIMARY KEY (`id`),
UNIQUE KEY `idx_row_lock_id` (`table_name`,`row_key`),
INDEX `idx_tranId_branchId`(`branch_id`, `trans_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='行锁';