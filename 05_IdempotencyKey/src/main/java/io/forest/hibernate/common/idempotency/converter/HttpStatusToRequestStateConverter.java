package io.forest.hibernate.common.idempotency.converter;

import org.springframework.http.ResponseEntity;

import io.forest.hibernate.common.idempotency.repository.State;

public class HttpStatusToRequestStateConverter {

	public State covert(ResponseEntity<?> responseEntity) {
		return responseEntity.getStatusCode()
				.is2xxSuccessful() ? State.SUCCEED : State.FAILED;
	}
}
