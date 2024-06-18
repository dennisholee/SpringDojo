package io.forest.txnoutbox.adapter.repository.sql.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@EntityListeners(ClaimEntityListener.class)
@Log4j2
@Data
@Entity
public class Claim {

	@Id
	UUID id;

	LocalDate submissionDate;

	String note;

	@PostPersist
	public void claimSaved() {
		log.info("Claim saved [id={}, submissionDate={}, note={}", id, submissionDate, note);
	}
}
