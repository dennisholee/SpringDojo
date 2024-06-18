package io.forest.txnoutbox.adapter.repository.sql.model;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import io.forest.txnoutbox.port.dto.ClaimCreatedEvent;
import io.forest.txnoutbox.port.dto.ClaimDto;
import jakarta.persistence.PrePersist;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ClaimEntityListener {

	@Autowired
	ApplicationEventPublisher applicationEventPublisher;

	@PrePersist
	private void afterCreate(Claim claim) {
		ClaimDto dto = new ClaimDto();
		dto.setId((UUID) claim.getId());
		dto.setNote(claim.getNote());
		dto.setSubmissionDate(claim.getSubmissionDate());

		ClaimCreatedEvent ce = new ClaimCreatedEvent(this);
		ce.setEventId(UUID.randomUUID());
		ce.setOccuredAt(LocalDate.now());
		ce.setType("ClaimCreatedEvent");
		ce.setClaimDto(dto);

		applicationEventPublisher.publishEvent(ce);
	}
}
