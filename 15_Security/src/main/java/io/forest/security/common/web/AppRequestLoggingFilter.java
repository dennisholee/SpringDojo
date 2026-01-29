package io.forest.security.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AppRequestLoggingFilter extends AbstractRequestLoggingFilter {
	@Override
	protected void beforeRequest(	HttpServletRequest request,
									String message) {

		String requestId = Optional.of(request)
			.map(HttpServletRequest::getRequestId)
			.orElse(UUID.randomUUID().toString());
		
		MDC.put("requestId", requestId);
	}

	@Override
	protected void afterRequest(HttpServletRequest request,
								String message) {
		MDC.clear();
		
	}
	
	@Override
	protected boolean shouldLog(HttpServletRequest request) {
		return List.of("/actuator")
				.stream()
				.noneMatch(request.getRequestURI()::startsWith);
	}


	
}
