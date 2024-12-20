package io.forest.redis.domain;

import java.util.UUID;

public class UserBuilder {

	UUID id;

	String firstName;

	String lastName;

	String email;

	public UserBuilder id(UUID id) {
		this.id = id;
		return this;
	}

	public UserBuilder name(String firstName,
							String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
		return this;
	}
	
	public UserBuilder firstName(String firstName) {
		this.firstName = firstName;
		return this;
	}
	
	public UserBuilder lastName(String lastName) {
		this.lastName = lastName;
		return this;

	}

	public UserBuilder email(String email) {
		this.email = email;
		return this;
	}

	public User build() {
		Name name = new Name(this.firstName, this.lastName);
		return new User(this.id, name, this.email);
	}
}
