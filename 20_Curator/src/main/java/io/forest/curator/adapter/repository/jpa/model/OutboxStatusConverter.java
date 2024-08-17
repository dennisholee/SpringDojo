package io.forest.curator.adapter.repository.jpa.model;

import java.util.Optional;

import io.forest.curator.common.model.Status;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OutboxStatusConverter implements AttributeConverter<Status, String> {

	@Override
	public String convertToDatabaseColumn(Status status) {
		return Optional.ofNullable(status)
				.map(Status::getCode)
				.orElse(null);
	}

	@Override
	public Status convertToEntityAttribute(String code) {
		return Optional.ofNullable(code)
				.map(Status::fromCode)
				.orElse(null);
	}
}
