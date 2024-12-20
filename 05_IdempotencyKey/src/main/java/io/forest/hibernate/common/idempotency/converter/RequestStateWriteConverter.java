package io.forest.hibernate.common.idempotency.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import io.forest.hibernate.common.idempotency.repository.State;

@WritingConverter
public class RequestStateWriteConverter implements Converter<State, String> {

	@Override
	public String convert(State source) {
		return source.getValue();
	}

}
