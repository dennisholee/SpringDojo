package com.acme.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:service-application.properties")
public class MyGateway {

	public static void main(String[] args) {
		SpringApplication.run(MyGateway.class, args);
	}
}
