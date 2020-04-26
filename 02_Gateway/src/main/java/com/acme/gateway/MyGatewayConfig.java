package com.acme.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyGatewayConfig {
	
	@Autowired
	private RouteLocatorBuilder builder;
	
//	@Value("${test.uri:http://httpbin.org:80}")
//	String uri;

	@Bean
	public RouteLocator myRouteLoactor() {
		return builder.routes()
			.route(r -> r.path("/joke")
				.filters(f -> 
					f.prefixPath("")
						.addResponseHeader("foo", "baz")
				).uri("http://localhost:8080/joke")
			).build();
	}
}
