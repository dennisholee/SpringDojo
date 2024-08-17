package io.forest.curator.common.model;

public enum Status {

	WAIT("W"),

	SENT("S"),

	DONE("D"),

	CANCEL("C");

	String code;

	Status(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}

	public static Status fromCode(String code) {
		return switch (code) {
		case "W" -> WAIT;
		case "S" -> SENT;
		case "D" -> DONE;
		case "C" -> CANCEL;
		default -> throw new RuntimeException();
		};
	}
}
