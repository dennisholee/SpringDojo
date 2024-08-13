package io.forest.concurrency.common.events;

public interface DomainEventPublisher {

	void publishEvent(Object event);
	
	
}
