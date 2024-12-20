package io.forest.hibernate.common.idempotency.repository;

import java.util.Optional;

public enum State {

	NEW("N"), WIP("W"), SUCCEED("S"), FAILED("F");

	String value;

	State(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static State of(String value) {
		return Optional.ofNullable(value)
				.map(it -> switch (it.toUpperCase()) {
				case "N" -> NEW;
				case "W" -> WIP;
				case "S" -> SUCCEED;
				case "F" -> FAILED;
				default -> null;
				})
				.orElseThrow(() -> new RuntimeException());
	}

	public interface TypeConstant {
		String NEW = "NEW";
		String WIP = "WIP";
		String SUCCEED = "SUCCEED";
		String FAILED = "FAILED";
	}
}
