package io.forest.openapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.forest.openapi.conf.SecurityConfig;
import io.forest.openapi.conf.UserRestAPIConfig;

@SpringBootApplication
@Import({ SecurityConfig.class, UserRestAPIConfig.class })
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
