package io.forest.hibernate.common.idempotency.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import io.forest.hibernate.common.idempotency.repository.State;

@ReadingConverter
public class RequestStateReadConverter implements Converter<String, State> {

	@Override
	public State convert(String source) {
		return State.of(source);
	}
}
