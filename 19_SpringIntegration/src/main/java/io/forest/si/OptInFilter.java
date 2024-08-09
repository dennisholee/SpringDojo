package io.forest.si;

import org.springframework.integration.annotation.Filter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class OptInFilter {

	@Filter
	public boolean accept(@Payload("#this['OptIn'].optIn") boolean optIn) {
		return optIn;
	}
}
