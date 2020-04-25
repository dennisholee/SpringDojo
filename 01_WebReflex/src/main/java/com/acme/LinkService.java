package com.acme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;

import reactor.core.publisher.Mono;

@Service
public class LinkService {

	private static Logger logger = LogManager.getLogger(LinkService.class);
	
	public Mono<String> joke(ServerRequest req) {
		logger.info("Request received!!!");
		return Mono.just("{'foo':'bar'}");
	}
}
