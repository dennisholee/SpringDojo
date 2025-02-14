package io.forest.openapi;

import lombok.CustomLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.forest.openapi.conf.SecurityConfig;
import io.forest.openapi.conf.UserRestAPIConfig;

@SpringBootApplication
@Import({ SecurityConfig.class, UserRestAPIConfig.class })
@CustomLog
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
