package com.sxb.lin.trx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.sxb.lin")
public class TestApplication {

	public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
	
}

