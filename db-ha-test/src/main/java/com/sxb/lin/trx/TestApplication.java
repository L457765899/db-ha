package com.sxb.lin.trx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan("com.sxb.lin.trx.db.dao")
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.sxb.lin")
public class TestApplication {

	public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
	
}

