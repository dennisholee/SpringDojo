package io.forest.concurrency.adapter.repository.model;

import java.util.UUID;

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

@Entity(name = "CONSENTS")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ConsentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "CONSENT_ID")
	UUID id;
	
	@Column(name = "OPT_IN", nullable = false)
	boolean optIn;
	
}
