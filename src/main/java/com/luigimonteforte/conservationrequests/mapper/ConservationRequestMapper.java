package com.luigimonteforte.conservationrequests.mapper;

import com.luigimonteforte.conservationrequests.entity.ConservationRequest;
import com.luigimonteforte.conservationrequests.model.ConservationRequestDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConservationRequestMapper {
    ConservationRequest toEntity(ConservationRequestDto conservationRequestDto);

    @AfterMapping
    default void linkDocuments(@MappingTarget ConservationRequest conservationRequest) {
        conservationRequest.getDocuments().forEach(document -> document.setConservationRequest(conservationRequest));
    }

    ConservationRequestDto toDto(ConservationRequest conservationRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ConservationRequest partialUpdate(ConservationRequestDto conservationRequestDto, @MappingTarget ConservationRequest conservationRequest);
}