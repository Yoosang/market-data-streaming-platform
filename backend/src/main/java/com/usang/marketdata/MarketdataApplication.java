package com.usang.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: @Scheduled 어노테이션이 동작하려면 반드시 필요
@SpringBootApplication
@EnableScheduling
public class MarketdataApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketdataApplication.class, args);
	}

}
