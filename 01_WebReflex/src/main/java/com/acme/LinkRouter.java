package com.acme;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class LinkRouter {
	
	@Autowired
	LinkService linkController;

	@Bean
	public RouterFunction<ServerResponse> joke() {
		
		return route(GET("/joke"), 
			    req -> ok()
			    	.contentType(MediaType.APPLICATION_JSON)
			    	.body(linkController.joke(req), String.class));
	}
}
