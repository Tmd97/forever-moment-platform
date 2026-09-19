package com.forvmom.data.dao;

import com.forvmom.data.entities.MediaVariant;
import com.forvmom.data.entities.MediaVariantType;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class MediaVariantDaoImpl extends GenericDaoImpl<MediaVariant, Long> implements MediaVariantDao {

    public MediaVariantDaoImpl() {
        super(MediaVariant.class);
    }

    @Override
    public MediaVariant findByMediaIdAndVariantType(Long mediaId, MediaVariantType variantType) {
        try {
            return em.createQuery(
                    "SELECT v FROM MediaVariant v WHERE v.media.id = :mediaId AND v.variantType = :variantType",
                    MediaVariant.class)
                    .setParameter("mediaId", mediaId)
                    .setParameter("variantType", variantType)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<MediaVariant> findByMediaId(Long mediaId) {
        return em.createQuery(
                "SELECT v FROM MediaVariant v WHERE v.media.id = :mediaId",
                MediaVariant.class)
                .setParameter("mediaId", mediaId)
                .getResultList();
    }

    @Override
    public List<MediaVariant> findByMediaIds(List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                "SELECT v FROM MediaVariant v JOIN FETCH v.media m WHERE m.id IN :mediaIds",
                MediaVariant.class)
                .setParameter("mediaIds", mediaIds)
                .getResultList();
    }

    @Override
    public void deleteByMediaId(Long mediaId) {
        em.createQuery("DELETE FROM MediaVariant v WHERE v.media.id = :mediaId")
                .setParameter("mediaId", mediaId)
                .executeUpdate();
    }

    @Override
    public String findFilePathByStorageFileName(String storageFileName) {
        try {
            return em.createQuery(
                    "SELECT v.filePath FROM MediaVariant v WHERE v.storageFileName = :storageFileName",
                    String.class)
                    .setParameter("storageFileName", storageFileName)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
