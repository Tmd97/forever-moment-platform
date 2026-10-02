package com.forvmom.data.dao;

import com.forvmom.data.entities.Faq;

import java.util.List;

public interface FaqDao extends GenericDao<Faq, Long> {

    /** All FAQs ordered for the admin list (includes inactive) */
    List<Faq> findAllOrdered();

    /** Only active FAQs ordered, for the public FAQ page */
    List<Faq> findAllActiveOrdered();

    boolean existsById(Long id);

    /** Highest display order currently in use, 0 when no FAQs exist yet */
    Integer getMaxDisplayOrder();
}
