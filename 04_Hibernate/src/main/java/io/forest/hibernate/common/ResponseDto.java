package io.forest.hibernate.common;

import java.util.Optional;
import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class ResponseDto<T> {

	@NonNull
	ResponseStatus responseStatus;

	@NonNull
	T data;

	String message;

	public <R> void ifOkOrElse(	Function<T, ?> funcOk,
								Function<T, R> funcErr) {

		Optional.ofNullable(this.responseStatus)
				.filter(ResponseStatus::isOK)
				.ifPresentOrElse(s -> funcOk.apply(this.data), () -> funcErr.apply(this.data));
	}
}
