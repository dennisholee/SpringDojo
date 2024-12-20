package io.forest.hibernate.common.idempotency.response;

import org.springframework.http.ResponseEntity;

public class FailedRequestStateResponse implements StateResponse {

	@Override
	public ResponseEntity<Object> toResponseEntity(Object body) {
		return ResponseEntity.unprocessableEntity()
				.body(body);
	}
}
