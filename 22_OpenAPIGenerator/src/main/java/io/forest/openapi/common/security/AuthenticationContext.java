package io.forest.openapi.common.security;

import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public interface AuthenticationContext {

	default Optional<String> getName() {
		Authentication authN = SecurityContextHolder.getContext()
				.getAuthentication();
		
		
		
		return authN instanceof AnonymousAuthenticationToken == false ? Optional.of(authN.getName()) : Optional.empty();
	}

}
