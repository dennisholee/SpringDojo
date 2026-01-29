package io.forest.security.security.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import java.util.Optional;

@RequiredArgsConstructor
public class AppRequestHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {

	@NonNull
	Boolean hasLocalProfile;
	
	@NonNull
	String envUserName;

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest servletRequest) {
		Object principal = Optional.of(servletRequest)
				.map(super::getPreAuthenticatedPrincipal)
				.orElse(System.getProperty(envUserName));
		
		
		MDC.put("principal", principal.toString());
		
		return principal;

	}

}
