package io.forest.hibernate.common;

public enum ResponseStatus {

	OK, PERSISTENCE;

	boolean isOK() {
		return true;
	}
}
