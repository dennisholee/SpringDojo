package io.forest.hibernate.common.idempotency.response;

import org.springframework.http.ResponseEntity;

public interface StateResponse {

	ResponseEntity<Object> toResponseEntity(Object body);

}
