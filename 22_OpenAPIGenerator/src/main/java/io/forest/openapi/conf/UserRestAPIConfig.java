package io.forest.openapi.conf;

import org.springframework.context.annotation.Bean;

import io.forest.openapi.adapter.web.UserQueryAdapter;
import io.forest.openapi.common.security.AuthenticationContext;

public class UserRestAPIConfig {
	
	@Bean
	UserQueryAdapter userQueryAdapter(AuthenticationContext authenticationContext) {
		return new UserQueryAdapter();
	}

}
