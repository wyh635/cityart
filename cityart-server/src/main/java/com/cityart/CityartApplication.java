package com.cityart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@EnableScheduling            //开启定时任务（超时订单自动取消、在展数量重算等）
@Slf4j
@EnableCaching
public class CityartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityartApplication.class, args);
        log.info("server started");
    }
}
