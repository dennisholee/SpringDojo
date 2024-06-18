package io.forest.txnoutbox.common;

import java.util.Optional;
import java.util.function.Function;

import lombok.Builder;

@Builder
public class ResponseDto<T, M, S extends ResponseStatus> {

	S responseStatus;

	T payload;

	M message;

	public S ifOkOrElse(Function<T, S> okFunc,
						Function<M, S> elseFunc) {

		return Optional.of(payload)
				.isEmpty() ? okFunc.apply(payload) : elseFunc.apply(message);
	}

	public M getMessage() {
		return this.message;
	}

	public T getPayload() {
		return this.payload;
	}
}
