package io.forest.txnoutbox.port.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimCreatedEvent extends ApplicationEvent {

	UUID eventId;

	String type;

	LocalDate occuredAt;

	ClaimDto claimDto;

	public ClaimCreatedEvent(Object source) {
		super(source);
	}

}
