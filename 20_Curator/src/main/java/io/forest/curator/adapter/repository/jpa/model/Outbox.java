package io.forest.curator.adapter.repository.jpa.model;

import java.util.UUID;

import io.forest.curator.common.model.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity(name = "OUTBOX")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class Outbox {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name="id")
	UUID id;
	
	@Column(name = "metadata")
	String metadata;

	@Column(name = "operation")
	String operation;

	@Column(name = "payload")
	String payload;
	
	@Column(name = "status")
	Status status;
}
