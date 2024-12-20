package io.forest.openapi.conf;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import io.forest.openapi.common.security.AuthenticationContext;
import io.forest.openapi.common.security.Authorizer;
import io.forest.openapi.common.security.DefaultAuthorizer;

@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	AuthenticationContext authenticationContext() {
		return new AuthenticationContext() {
		};
	}

	@Bean("authorizer")
	public Authorizer authorizer(AuthenticationContext authenticationContext) {
		return new DefaultAuthorizer(authenticationContext);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.cors(corsCustomizer -> corsCustomizer.disable())
				.authorizeHttpRequests(authz -> authz.anyRequest()
						.authenticated())
				.httpBasic(withDefaults())
				.csrf(crsfCustomizer -> crsfCustomizer.disable())
				.build();
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		return new InMemoryUserDetailsManager(
				User.withDefaultPasswordEncoder()
						.username("adam")
						.password("password")
						.roles("USER")
						.build(),
				User.withDefaultPasswordEncoder()
						.username("john")
						.password("password")
						.roles("USER")
						.build());
	}
}
