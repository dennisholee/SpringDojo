package io.forest.simple;

import org.springframework.stereotype.Component;

import graphql.kickstart.tools.GraphQLQueryResolver;

@Component
public class Query implements GraphQLQueryResolver {
	public String helloWorld() {
		return "Hello World";
	}
}
