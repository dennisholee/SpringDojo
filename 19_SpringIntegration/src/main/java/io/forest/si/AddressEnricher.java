package io.forest.si;

import java.util.Optional;
import java.util.UUID;

import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class AddressEnricher {

	@NonNull
	AddressesH2Repository repository;

	@Transformer(inputChannel = "AddressEnricher.inChannel"/*
															 * , outputChannel = "AddressEnricher.outChannel"
															 */)
	public Optional<Address> enrich(@Payload("id") UUID userId) {

		log.info("Searching user [userId={}]", userId);

		Optional<Address> customer = repository.findByCustomerId(userId);

		log.info("Customer search result [userId={}, record={}]", userId, customer.get());

		return customer;
	}
}
