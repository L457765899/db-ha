package com.sxb.lin.trx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1)
@SpringBootApplication(scanBasePackages = "com.sxb")
public class TestApplication {

	public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
	
}

