package io.forest.openapi.common.security;

import java.util.function.Predicate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DefaultAuthorizer implements Authorizer {

	@NonNull
	AuthenticationContext authenticationContext;

	@Override
	public boolean hasPermission(String permission) {
		
		log.info("Verifying permission [permission={}] for user [name={}]", permission, authenticationContext.getName());
		
		return authenticationContext.getName()
				.filter(isUserEntitled)
				.isPresent();
	}

	Predicate<String> isUserEntitled = name -> switch (name) {
	case "adam" -> true;
	default -> false;
	};
}
