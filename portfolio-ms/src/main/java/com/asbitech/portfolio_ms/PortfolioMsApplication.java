package com.asbitech.portfolio_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PortfolioMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfolioMsApplication.class, args);
	}

}
