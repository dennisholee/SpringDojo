package io.forest.hibernate.common.idempotency.key;

import java.util.Optional;
import java.util.UUID;

public class UUIDIdempotencyKeyValidator implements IdempotencyKeyValidator {

	@Override
	public boolean isValid(Object value) {

		try {
			return Optional.ofNullable(value)
					.map(String::valueOf)
					.map(UUID::fromString)
					.isPresent();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}
