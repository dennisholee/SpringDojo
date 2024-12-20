package io.forest.redis.config;

import org.springframework.context.annotation.Bean;

import io.forest.redis.app.RegisterUserApp;
import io.forest.redis.app.SearchUserApp;
import io.forest.redis.port.RegisterUser;
import io.forest.redis.port.SearchUser;
import io.forest.redis.port.UserRepository;

public class AppConfig {

//	@Bean
//	RegisterUser registerUser(UserRepository userRepository) {
//		return new RegisterUserApp(userRepository);
//	}
	
	@Bean
	RegisterUser registerUser(UserRepository userRepository) {
		return new RegisterUserApp(userRepository);
	}

	@Bean
	SearchUser searchUser(UserRepository userRepository) {
		return new SearchUserApp(userRepository);
	}
}
