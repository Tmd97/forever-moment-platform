package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.FaqRequestDto;
import com.forvmom.common.dto.response.FaqResponseDto;
import com.forvmom.data.entities.Faq;

import java.util.List;
import java.util.stream.Collectors;

public class FaqBeanMapper {

    private FaqBeanMapper() {
    }

    public static Faq mapRequestToEntity(FaqRequestDto dto) {
        Faq entity = new Faq();
        return updateEntityFromRequest(entity, dto);
    }

    public static Faq updateEntityFromRequest(Faq entity, FaqRequestDto dto) {
        entity.setQuestion(dto.getQuestion());
        entity.setAnswer(dto.getAnswer());
        if (dto.getDisplayOrder() != null) {
            entity.setDisplayOrder(dto.getDisplayOrder());
        }
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);
        return entity;
    }

    public static FaqResponseDto mapEntityToDto(Faq entity) {
        FaqResponseDto dto = new FaqResponseDto();
        dto.setId(entity.getId());
        dto.setQuestion(entity.getQuestion());
        dto.setAnswer(entity.getAnswer());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    public static List<FaqResponseDto> mapEntitiesToDtos(List<Faq> entities) {
        return entities.stream().map(FaqBeanMapper::mapEntityToDto).collect(Collectors.toList());
    }
}
