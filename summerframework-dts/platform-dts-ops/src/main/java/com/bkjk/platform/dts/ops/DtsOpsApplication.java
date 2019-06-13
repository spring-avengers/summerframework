package com.bkjk.platform.dts.ops;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = {"com.bkjk.platform.dts.ops.common.mapper"}, annotationClass = Mapper.class)
@EnableDiscoveryClient
@EnableScheduling
public class DtsOpsApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DtsOpsApplication.class, args);
    }
}
