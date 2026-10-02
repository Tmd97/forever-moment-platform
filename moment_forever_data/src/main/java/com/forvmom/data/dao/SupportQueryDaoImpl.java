package com.forvmom.data.dao;

import com.forvmom.data.entities.SupportQuery;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class SupportQueryDaoImpl extends GenericDaoImpl<SupportQuery, Long> implements SupportQueryDao {

    public SupportQueryDaoImpl() {
        super(SupportQuery.class);
    }

    @Override
    public List<SupportQuery> findAll(String status) {
        String jpql = "SELECT s FROM SupportQuery s WHERE s.deleted = false"
                + (status != null ? " AND s.status = :status" : "")
                + " ORDER BY s.createdOn DESC";
        TypedQuery<SupportQuery> query = em.createQuery(jpql, SupportQuery.class);
        if (status != null) {
            query.setParameter("status", status);
        }
        return query.getResultList();
    }

    @Override
    public List<SupportQuery> findByApplicationUserId(Long applicationUserId) {
        return em.createQuery(
                "SELECT s FROM SupportQuery s WHERE s.deleted = false AND s.applicationUser.id = :userId "
                        + "ORDER BY s.createdOn DESC",
                SupportQuery.class)
                .setParameter("userId", applicationUserId)
                .getResultList();
    }
}
