package com.forvmom.data.dao.impl;

import com.forvmom.data.dao.PromotionAssetDao;
import com.forvmom.data.dao.GenericDaoImpl;
import com.forvmom.data.entities.PromotionAsset;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PromotionAssetDaoImpl extends GenericDaoImpl<PromotionAsset, Long> implements PromotionAssetDao {

    public PromotionAssetDaoImpl() {
        super(PromotionAsset.class);
    }

    @Override
    public List<PromotionAsset> findByFilters(String promoKey, String placement, Boolean isActive) {
        StringBuilder jpql = new StringBuilder(
                "SELECT p FROM PromotionAsset p JOIN FETCH p.media m WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (promoKey != null && !promoKey.isBlank()) {
            jpql.append(" AND lower(p.promoKey) = :promoKey");
            params.put("promoKey", promoKey.toLowerCase());
        }
        if (placement != null && !placement.isBlank()) {
            jpql.append(" AND lower(p.placement) = :placement");
            params.put("placement", placement.toLowerCase());
        }
        if (isActive != null) {
            jpql.append(" AND p.isActive = :isActive");
            params.put("isActive", isActive);
        }

        jpql.append(" ORDER BY p.promoKey ASC, p.placement ASC, p.priority ASC, p.createdOn DESC");
        TypedQuery<PromotionAsset> query = em.createQuery(jpql.toString(), PromotionAsset.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public List<PromotionAsset> findActiveByKeyAndPlacement(String promoKey, String placement, LocalDateTime now) {
        StringBuilder jpql = new StringBuilder(
                "SELECT p FROM PromotionAsset p JOIN FETCH p.media m WHERE p.isActive = true "
                        + "AND lower(p.promoKey) = :promoKey "
                        + "AND (:placement IS NULL OR lower(p.placement) = :placement) "
                        + "AND (p.startAt IS NULL OR p.startAt <= :now) "
                        + "AND (p.endAt IS NULL OR p.endAt >= :now) "
                        + "ORDER BY p.priority ASC, p.createdOn DESC");

        TypedQuery<PromotionAsset> query = em.createQuery(jpql.toString(), PromotionAsset.class)
                .setParameter("promoKey", promoKey.toLowerCase())
                .setParameter("placement", placement == null || placement.isBlank() ? null : placement.toLowerCase())
                .setParameter("now", now);
        return query.getResultList();
    }

    @Override
    public boolean existsByMediaId(Long mediaId) {
        Long count = em.createQuery(
                "SELECT count(p.id) FROM PromotionAsset p WHERE p.media.id = :mediaId",
                Long.class).setParameter("mediaId", mediaId).getSingleResult();
        return count != null && count > 0;
    }
}
