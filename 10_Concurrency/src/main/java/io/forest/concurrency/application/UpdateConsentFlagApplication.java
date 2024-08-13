package io.forest.concurrency.application;

import java.util.Optional;
import java.util.function.Function;

import io.forest.concurrency.domain.model.Consent;
import io.forest.concurrency.domain.model.ConsentBuilder;
import io.forest.concurrency.port.ConsentsRepository;
import io.forest.concurrency.port.UpdateConsentCommand;
import io.forest.concurrency.port.UpdateConsentFlag;
import io.forest.concurrency.port.dto.ConsentDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class UpdateConsentFlagApplication implements UpdateConsentFlag {

	@NonNull
	ConsentsRepository repository;

	@NonNull
	ConsentMapper consentMapper;

	@NonNull
	ConsentBuilder consentBuilder;

	@Override
	public void handleRequest(UpdateConsentCommand command) {
		Mono.just(command)
				.map(UpdateConsentCommand::getId)
				.doOnNext(it -> log.info("Preparing to search consent [id={}]", it))
				.map(repository::findById)
				.doOnNext(it -> log.info("Consent search result [record={}]", it))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(entityToDomain)
				.doOnNext(it -> log.info("Preparing to toggle consent [record={}]", it))
				.doOnNext(it -> it.disableConsent())
				.subscribe();
	}

	Function<ConsentDTO, Consent> entityToDomain = dto -> {
		return this.consentBuilder.setConsentId(dto.getId())
				.setOptIn(dto.isOptIn())
				.build();
	};
}
