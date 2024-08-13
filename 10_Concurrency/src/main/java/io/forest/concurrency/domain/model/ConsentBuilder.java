package io.forest.concurrency.domain.model;

import java.util.UUID;

import io.forest.concurrency.common.events.DomainEventPublisher;

public class ConsentBuilder {

	UUID consentId;

	boolean optIn = false;

	private DomainEventPublisher domainEventPublisher;

	public ConsentBuilder(DomainEventPublisher domainEventPublisher) {
		this.domainEventPublisher = domainEventPublisher;
	}

	public ConsentBuilder setConsentId(UUID consentId) {
		this.consentId = consentId;
		return this;
	}

	public ConsentBuilder setOptIn(boolean optIn) {
		this.optIn = optIn;
		return this;
	}

	public Consent build() {
		Consent consent = new Consent();
		consent.setDomainEventPublisher(domainEventPublisher);
		consent.setConsentId(this.consentId);
		consent.setFlag(this.optIn);
		return consent;
	}
}
