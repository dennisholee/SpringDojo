package io.forest.si;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class OptInEnricher {

	@NonNull
	ConsentsH2Repository repository;

	@Transformer(	inputChannel = "OptInEnricher.inChannel",
					outputChannel = "OptInEnricher.outChannel")
	public OptInFlag enrich(@Payload("id") UUID userId) {
		Consents consent = this.repository.findByCustomerIdAndConsentType(userId, ConsentType.OPT_IN);

		log.info("Find user and consent result [userId={}, consent={}]", userId, consent);
		
		return Optional.ofNullable(consent)
				.map(consentToOptIn)
				.orElse(new OptInFlag());
	}

	Function<Consents, OptInFlag> consentToOptIn = c -> new OptInFlag()
			.setOptIn(ConsentType.OPT_IN.equals(c.getConsentType()));
}
