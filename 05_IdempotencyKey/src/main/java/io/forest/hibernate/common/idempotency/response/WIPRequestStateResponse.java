package io.forest.hibernate.common.idempotency.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class WIPRequestStateResponse implements StateResponse {

	@Override
	public ResponseEntity<Object> toResponseEntity(Object body) {
		return ResponseEntity.status(HttpStatus.TOO_EARLY)
				.body("Please try later");
	}
}
