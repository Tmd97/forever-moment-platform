package com.forvmom.data.dao;

import com.forvmom.data.entities.SupportQuery;

import java.util.List;

public interface SupportQueryDao extends GenericDao<SupportQuery, Long> {

    /** All queries, newest first, optionally filtered by status (admin list) */
    List<SupportQuery> findAll(String status);

    /** Queries submitted by a given application user, newest first */
    List<SupportQuery> findByApplicationUserId(Long applicationUserId);
}
