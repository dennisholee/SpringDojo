package com.acme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = App.class)
public class LinkServiceTest {
	
	@Autowired
    private LinkRouter config;
	
	@Test
	void test() {
		WebTestClient client = WebTestClient
	            .bindToRouterFunction(config.joke())
	            .build();
		
		client.get()
			.uri("/joke")
			.accept(MediaType.APPLICATION_PROBLEM_JSON)
			.exchange()
			.expectStatus()
			.is2xxSuccessful()
			.expectBody()
			.json("{'foo':'bar'}");
	}

}
