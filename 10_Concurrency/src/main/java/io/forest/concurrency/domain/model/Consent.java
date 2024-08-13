package io.forest.concurrency.domain.model;

import static lombok.AccessLevel.PACKAGE;

import java.util.UUID;

import io.forest.concurrency.common.events.DomainEventPublisher;
import io.forest.concurrency.domain.event.ConsentDisabledEvent;
import io.forest.concurrency.domain.event.ConsentEnabledEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor(access = PACKAGE)
@NoArgsConstructor(access = PACKAGE)
@Getter
@Setter(PACKAGE)
@EqualsAndHashCode
@ToString
@Log4j2
public class Consent {

	UUID consentId;

	DomainEventPublisher domainEventPublisher;

	boolean flag = false;

	public void enableConsent() {
		if (this.flag == false) {
			this.flag = true;

			log.info("Consent flag updated [flat={}]", this.flag);

			ConsentEnabledEvent consentEnabledEvent = new ConsentEnabledEvent();
			this.domainEventPublisher.publishEvent(consentEnabledEvent);

			log.info("ConsentEnabledEvent dispatched [event={}]", consentEnabledEvent);
		}
	}

	public void disableConsent() {
		if (this.flag == true) {
			this.flag = false;

			log.info("Consent flag updated [flat={}]", this.flag);

			ConsentDisabledEvent consentDisabledEvent = new ConsentDisabledEvent();
			this.domainEventPublisher.publishEvent(consentDisabledEvent);

			log.info("ConsentDisabledEvent dispatched [event={}]", consentDisabledEvent);
		}
	}
}
