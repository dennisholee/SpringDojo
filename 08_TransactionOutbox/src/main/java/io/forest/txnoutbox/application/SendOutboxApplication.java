package io.forest.txnoutbox.application;

import org.springframework.context.event.EventListener;

import io.forest.txnoutbox.port.dto.OutboxItemCreatedEvent;

public class SendOutboxApplication {

	
	
	@EventListener
	public void handleEvent(OutboxItemCreatedEvent event) {

	}
}
