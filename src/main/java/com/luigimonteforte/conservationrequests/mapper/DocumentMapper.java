package com.luigimonteforte.conservationrequests.mapper;

import com.luigimonteforte.conservationrequests.entity.Document;
import com.luigimonteforte.conservationrequests.model.DocumentDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {
    Document toEntity(DocumentDto documentDto);

    DocumentDto toDto(Document document);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Document partialUpdate(DocumentDto documentDto, @MappingTarget Document document);
}