package com.luigimonteforte.conservationrequests.mapper;

import com.luigimonteforte.conservationrequests.entity.RequestStatusHistory;
import com.luigimonteforte.conservationrequests.model.RequestStatusHistoryDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RequestStatusHistoryMapper {
    RequestStatusHistoryDto toDto(RequestStatusHistory requestStatusHistory);
}