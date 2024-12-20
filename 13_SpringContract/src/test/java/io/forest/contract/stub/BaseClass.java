package io.forest.contract.stub;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public abstract class BaseClass {

	@Autowired
	WebApplicationContext context;

	@BeforeEach
	void setup() {
		RestAssuredMockMvc.webAppContextSetup(context);
	}
}
