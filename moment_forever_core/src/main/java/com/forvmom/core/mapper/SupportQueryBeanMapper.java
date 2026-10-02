package com.forvmom.core.mapper;

import com.forvmom.common.dto.response.SupportQueryResponseDto;
import com.forvmom.data.entities.SupportQuery;

import java.util.List;
import java.util.stream.Collectors;

public class SupportQueryBeanMapper {

    private SupportQueryBeanMapper() {
    }

    public static SupportQueryResponseDto mapEntityToDto(SupportQuery entity) {
        SupportQueryResponseDto dto = new SupportQueryResponseDto();
        dto.setId(entity.getId());
        dto.setReferenceId(entity.getReferenceId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setSubject(entity.getSubject());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setResolvedOn(entity.getResolvedOn());
        return dto;
    }

    public static List<SupportQueryResponseDto> mapEntitiesToDtos(List<SupportQuery> entities) {
        return entities.stream().map(SupportQueryBeanMapper::mapEntityToDto).collect(Collectors.toList());
    }
}
