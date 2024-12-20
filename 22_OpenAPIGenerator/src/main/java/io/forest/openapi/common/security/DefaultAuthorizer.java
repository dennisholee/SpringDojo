package io.forest.openapi.common.security;

import java.util.function.Predicate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
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
