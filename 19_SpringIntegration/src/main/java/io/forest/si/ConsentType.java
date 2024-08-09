package io.forest.si;

import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum ConsentType {

	OPT_IN("I"), OPT_OUT("O");

	@NonNull
	String value;

	public static ConsentType from(String value) {
		return Objects.isNull(value) ? null
				: Optional.of(value)
						.map(v -> switch (v) {
						case "I" -> OPT_IN;
						case "O" -> OPT_OUT;
						default -> throw new IllegalArgumentException(String.format("Unexpected value [%s]", v));
						})
						.get();
	}
}
