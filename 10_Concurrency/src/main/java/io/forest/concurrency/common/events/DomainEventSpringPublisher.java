package io.forest.concurrency.common.events;

import org.springframework.context.ApplicationEventPublisher;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DomainEventSpringPublisher implements DomainEventPublisher {

	@NonNull
	ApplicationEventPublisher applicationEventPublisher;
	
	@Override
	public void publishEvent(Object event) {
		this.applicationEventPublisher.publishEvent(event);
	}
	
}
