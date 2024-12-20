package io.forest.hibernate.common.idempotency.response;

import org.springframework.http.ResponseEntity;

public class SuccessRequestStateResponse implements StateResponse {

	@Override
	public ResponseEntity<Object> toResponseEntity(Object body) {
		return ResponseEntity.ok()
				.body(body);
	}

}
