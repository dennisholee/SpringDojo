package io.forest.txnoutbox.port.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboxItemCreatedEvent extends ApplicationEvent {

	UUID eventId;

	String type;

	LocalDate occuredAt;

	OutboxDto outboxDto;

	public OutboxItemCreatedEvent(Object source) {
		super(source);
	}

}
