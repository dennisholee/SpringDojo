package io.forest.concurrency.adapter.repository;

import org.mapstruct.Mapper;

import io.forest.concurrency.adapter.repository.model.ConsentEntity;
import io.forest.concurrency.port.dto.ConsentDTO;

@Mapper
public interface ConsentMapper {

//	@Mapping(source = "id", target = "id")
//	@Mapping(source = "optIn", target = "optIn")
	default ConsentDTO map(ConsentEntity entity) {
		ConsentDTO dto = new ConsentDTO();
		dto.setId(entity.getId());
		dto.setOptIn(entity.isOptIn());
		return dto;
	}
}
