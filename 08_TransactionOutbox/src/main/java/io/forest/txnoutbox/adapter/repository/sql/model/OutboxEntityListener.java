package io.forest.txnoutbox.adapter.repository.sql.model;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.forest.txnoutbox.port.dto.OutboxDto;
import io.forest.txnoutbox.port.dto.OutboxItemCreatedEvent;
import jakarta.persistence.PrePersist;

public class OutboxEntityListener {

	@Autowired
	ApplicationEventPublisher applicationEventPublisher;

	@PrePersist
	private void afterCreate(Outbox outbox) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		OutboxDto outboxDto = null;

		try {
			outboxDto = objectMapper.readValue(outbox.getPayload(), OutboxDto.class);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		OutboxItemCreatedEvent event = new OutboxItemCreatedEvent(this);
		event.setEventId(UUID.randomUUID());
		event.setOccuredAt(LocalDate.now());
		event.setType("OutboxItemCreatedEvent");
		event.setOutboxDto(outboxDto);

		applicationEventPublisher.publishEvent(event);	}
}
