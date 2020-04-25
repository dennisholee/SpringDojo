package com.acme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
	
	private static Logger logger = LogManager.getLogger(App.class);
	
	public static void main(String[] args) {
		logger.info("Loading application ....");
		SpringApplication.run(App.class, args);
	}

}
