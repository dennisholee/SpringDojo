package io.forest.hibernate.common.idempotency.response;

import io.forest.hibernate.common.idempotency.repository.State;

public interface StateResponseFactory {

	StateResponse createResponse(State requestState);
}
