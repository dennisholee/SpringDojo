package io.forest.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.forest.redis.config.AppConfig;
import io.forest.redis.config.RedisConfig;
import io.forest.redis.config.RepositoryConfig;
import io.forest.redis.config.WebConfig;

@SpringBootApplication
@Import({ AppConfig.class, RedisConfig.class, RepositoryConfig.class, WebConfig.class })
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
