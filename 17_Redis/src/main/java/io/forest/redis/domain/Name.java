package io.forest.redis.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import lombok.Getter;

@ValueObject
@Getter
public class Name {

	String firstName;

	String lastName;
	
	public Name(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
