package io.forest.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

//import graphql.Scalars;
//import graphql.schema.GraphQLObjectType;
//import graphql.schema.GraphQLSchema;

@SpringBootApplication
public class SampleSimpleApplication {


	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleSimpleApplication.class, args);
	}

//	@Bean
//	GraphQLSchema schema() {
//		return GraphQLSchema.newSchema()
//				.query(GraphQLObjectType.newObject()
//						.name("query")
//						.field(f -> f.name("test").type(Scalars.GraphQLString)))
//				.build();
//	}

}
