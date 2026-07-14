package com.luigimonteforte.conservationrequests.mapper;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RequestMapper {
    Request toEntity(RequestDto requestDto);

    @AfterMapping
    default void linkDocuments(@MappingTarget Request request) {
        request.getDocuments().forEach(document -> document.setRequest(request));
    }

    RequestDto toDto(Request request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Request partialUpdate(RequestDto requestDto, @MappingTarget Request request);
}