package io.forest.concurrency.application;

import org.springframework.context.event.EventListener;

import io.forest.concurrency.domain.event.ConsentDisabledEvent;
import io.forest.concurrency.domain.event.ConsentEnabledEvent;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UpdateBetaApplication {

	@EventListener
	public void handle(ConsentEnabledEvent event) {
		log.info("ConsentEnabledEvent received [event={}]", event);
	}

	@EventListener
	public void handle(ConsentDisabledEvent event) {
		log.info("ConsentDisabledEvent received [event={}]", event);
	}
}
