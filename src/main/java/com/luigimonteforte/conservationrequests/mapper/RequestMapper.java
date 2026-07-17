package com.luigimonteforte.conservationrequests.mapper;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = DocumentMapper.class)
public interface RequestMapper {
	Request toEntity(CreateRequestDto requestDto);

	@AfterMapping
	default void linkDocuments(@MappingTarget Request request) {
		request.getDocuments().forEach(document -> document.setRequest(request));
	}

	RequestDto toDto(Request request);
}