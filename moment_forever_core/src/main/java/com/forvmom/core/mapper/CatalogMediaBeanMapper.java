package com.forvmom.core.mapper;

import com.forvmom.common.dto.response.ExperienceMediaResponseDto;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.data.entities.Media;
import com.forvmom.data.entities.MediaAttachmentEntity;

/**
 * Stateless mapper for any MediaAttachmentEntity (CategoryMediaMapper,
 * SubCategoryMediaMapper, ...) -> ExperienceMediaResponseDto.
 *
 * Reuses the existing ExperienceMediaResponseDto shape instead of a
 * duplicate DTO, and mirrors ExperienceMediaBeanMapper's conventions so the
 * same response contract and URL-building rules apply to category and
 * sub-category media as they do to experience media.
 */
public class CatalogMediaBeanMapper {

    private CatalogMediaBeanMapper() {
    }

    public static ExperienceMediaResponseDto mapEntityToDto(MediaAttachmentEntity mapper, ImageUrlConfig urlConfig) {
        if (mapper == null)
            return null;
        ExperienceMediaResponseDto dto = new ExperienceMediaResponseDto();
        dto.setMapperId(mapper.getId());
        dto.setDisplayOrder(mapper.getDisplayOrder());
        dto.setIsPrimary(mapper.getIsPrimary());
        dto.setIsActive(mapper.getIsActive());

        Media media = mapper.getMedia();
        if (media != null) {
            dto.setMediaId(media.getId());
            dto.setFileName(media.getFileName());
            dto.setStorageFileName(media.getStorageFileName());
            dto.setMimeType(media.getMimeType());
            dto.setFileSizeBytes(media.getFileSizeBytes());

            // Effective alt text: junction override wins, falls back to master
            dto.setAltText(mapper.getAltText() != null ? mapper.getAltText() : media.getAltText());

            if (media.getStorageFileName() != null) {
                dto.setUrl(urlConfig.buildPublicUrl(media.getStorageFileName()));
                dto.setThumbnailUrl(urlConfig.buildThumbnailUrl(media.getStorageFileName()));
            }
        }
        return dto;
    }
}
