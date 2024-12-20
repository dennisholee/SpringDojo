package io.forest.hibernate.common.idempotency.key;

public interface IdempotencyKeyValidator {

	boolean isValid(Object value);
	
}
