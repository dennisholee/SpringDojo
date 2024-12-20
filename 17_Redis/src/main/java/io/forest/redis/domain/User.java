package io.forest.redis.domain;

import java.util.UUID;

import org.jmolecules.ddd.annotation.AggregateRoot;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@AggregateRoot
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Log4j2
public class User {

	UUID id;
	
	Name name;
	
	String email;
	
	public User register() {
		this.id = UUID.randomUUID();
		log.info("User registered [user={}]", this);
		return this;
	}

}
