DROP TABLE IF EXISTS `dts_global_record`;

CREATE TABLE `dts_global_record` (
  `trans_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `client_info` varchar(200) COLLATE utf8_bin NOT NULL DEFAULT '',
  `client_ip` varchar(200) COLLATE utf8_bin NOT NULL DEFAULT '',
  `state` tinyint(1) NOT NULL COMMENT ' Begin(1),Committed(2),Rollbacked(3),CmmittedFailed(4),RollbackFailed(5),Commiting(6),Rollbacking(7);',
  `gmt_created` datetime NOT NULL,
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`trans_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='全局事务表';


DROP TABLE IF EXISTS `dts_branch_record`;

CREATE TABLE `dts_branch_record` (
  `branch_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `trans_id` bigint(20) NOT NULL,
  `resource_ip` varchar(200) COLLATE utf8_bin NOT NULL DEFAULT '',
  `resource_info` varchar(200) COLLATE utf8_bin NOT NULL DEFAULT '',
  `state` tinyint(1) NOT NULL COMMENT ' Begin(1), Success(2),Failed(3);',
  `gmt_created` datetime NOT NULL,
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`branch_id`),
  KEY `tx_id` (`trans_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='事务分支记录表';


DROP TABLE IF EXISTS `dts_branch_error_log`;

CREATE TABLE `dts_branch_error_log` (
  `branch_id` bigint(20) NOT NULL,
  `trans_id` bigint(20) NOT NULL,
  `resource_ip` varchar(200) COLLATE utf8_bin NOT NULL,
  `resource_info` varchar(200) COLLATE utf8_bin NOT NULL,
  `state` tinyint(1) NOT NULL DEFAULT '0',
  `gmt_created` datetime NOT NULL,
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_notify` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`branch_id`),
  UNIQUE KEY `branch_id` (`branch_id`),
  KEY `tx_id` (`trans_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='事务分支错误日志表';