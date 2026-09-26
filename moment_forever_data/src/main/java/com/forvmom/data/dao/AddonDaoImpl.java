package com.forvmom.data.dao;

import com.forvmom.data.entities.Addon;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class AddonDaoImpl extends GenericDaoImpl<Addon, Long> implements AddonDao {

    public AddonDaoImpl() {
        super(Addon.class);
    }

    @Override
    public List<Addon> findAll() {
        return em.createQuery(
                "SELECT a FROM Addon a " +
                        "LEFT JOIN FETCH a.imageMedia m " +
                        "WHERE a.deleted = false ORDER BY a.name ASC",
                Addon.class).getResultList();
    }

    @Override
    public boolean existsById(Long id) {
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM Addon a WHERE a.id = :id AND a.deleted = false",
                Long.class).setParameter("id", id).getSingleResult();
        return count > 0;
    }
}
