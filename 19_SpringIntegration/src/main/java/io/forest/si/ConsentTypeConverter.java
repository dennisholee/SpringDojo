package io.forest.si;

import java.util.Optional;

import jakarta.persistence.AttributeConverter;

public class ConsentTypeConverter implements AttributeConverter<ConsentType, String> {

	@Override
	public String convertToDatabaseColumn(ConsentType attribute) {
		return Optional.ofNullable(attribute)
				.map(ConsentType::getValue)
				.orElse(null);
	}

	@Override
	public ConsentType convertToEntityAttribute(String value) {
		return Optional.ofNullable(value)
				.map(ConsentType::from)
				.orElse(null);
	}

}
