package com.forvmom.data.dao;

import com.forvmom.data.entities.CategoryMediaMapper;

import java.util.List;

public interface CategoryMediaMapperDao extends GenericDao<CategoryMediaMapper, Long> {

    List<CategoryMediaMapper> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndMediaId(Long categoryId, Long mediaId);

    CategoryMediaMapper findByCategoryIdAndMediaId(Long categoryId, Long mediaId);

    CategoryMediaMapper findPrimaryByCategoryId(Long categoryId);

    List<CategoryMediaMapper> findPrimaryByCategoryIds(List<Long> categoryIds);

    List<CategoryMediaMapper> findByCategoryIds(List<Long> categoryIds);
}
