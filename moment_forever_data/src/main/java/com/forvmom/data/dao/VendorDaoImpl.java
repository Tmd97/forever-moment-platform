package com.forvmom.data.dao;

import com.forvmom.common.enums.VendorStatus;
import com.forvmom.data.entities.Vendor;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class VendorDaoImpl extends GenericDaoImpl<Vendor, Long> implements VendorDao {

    public VendorDaoImpl() {
        super(Vendor.class);
    }

    @Override
    public Optional<Vendor> findByAuthUserId(Long authUserId) {
        TypedQuery<Vendor> query = em.createQuery(
                "SELECT v FROM Vendor v WHERE v.authUser.id = :authUserId",
                Vendor.class);
        query.setParameter("authUserId", authUserId);
        try {
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Vendor> findByContactEmailIgnoreCase(String email) {
        TypedQuery<Vendor> query = em.createQuery(
                "SELECT v FROM Vendor v WHERE LOWER(v.contactEmail) = LOWER(:email)",
                Vendor.class);
        query.setParameter("email", email);
        try {
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Vendor> findByVendorCode(String vendorCode) {
        TypedQuery<Vendor> query = em.createQuery(
                "SELECT v FROM Vendor v WHERE v.vendorCode = :vendorCode",
                Vendor.class);
        query.setParameter("vendorCode", vendorCode);
        try {
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByContactEmailIgnoreCase(String email) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(v) FROM Vendor v WHERE LOWER(v.contactEmail) = LOWER(:email)",
                Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }

    @Override
    public boolean existsByVendorCode(String vendorCode) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(v) FROM Vendor v WHERE v.vendorCode = :vendorCode",
                Long.class);
        query.setParameter("vendorCode", vendorCode);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<Vendor> searchVendors(String searchTerm, String category, VendorStatus status) {
        StringBuilder jpql = new StringBuilder("SELECT v FROM Vendor v WHERE 1=1 ");

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            jpql.append("AND (LOWER(v.businessName) LIKE :term OR LOWER(v.contactName) LIKE :term OR LOWER(v.contactEmail) LIKE :term OR LOWER(v.vendorCode) LIKE :term) ");
        }

        if (category != null && !category.trim().isEmpty()) {
            jpql.append("AND LOWER(v.category) = LOWER(:category) ");
        }

        if (status != null) {
            jpql.append("AND v.status = :status ");
        }

        jpql.append("ORDER BY v.id DESC");

        TypedQuery<Vendor> query = em.createQuery(jpql.toString(), Vendor.class);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.setParameter("term", "%" + searchTerm.trim().toLowerCase() + "%");
        }
        if (category != null && !category.trim().isEmpty()) {
            query.setParameter("category", category.trim());
        }
        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }
}