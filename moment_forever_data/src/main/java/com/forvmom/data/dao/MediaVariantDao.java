package com.forvmom.data.dao;

import com.forvmom.data.entities.MediaVariant;
import com.forvmom.data.entities.MediaVariantType;

import java.util.List;

public interface MediaVariantDao extends GenericDao<MediaVariant, Long> {

    MediaVariant findByMediaIdAndVariantType(Long mediaId, MediaVariantType variantType);

    List<MediaVariant> findByMediaId(Long mediaId);

    List<MediaVariant> findByMediaIds(List<Long> mediaIds);

    void deleteByMediaId(Long mediaId);

    String findFilePathByStorageFileName(String storageFileName);
}
