package io.forest.txnoutbox.adapter.repository.sql.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.Data;

@EntityListeners(OutboxEntityListener.class)
@Entity(name = "OUTBOX")
@Data
public class Outbox {

	@Id
	UUID id;

	@Column(name = "metadata")
	String metadata;

	@Column(name = "operation")
	String operation;

	@Column(name = "payload")
	String payload;
}
