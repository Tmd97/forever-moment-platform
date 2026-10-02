package com.forvmom.data.dao;

import com.forvmom.data.entities.SubCategoryMediaMapper;

import java.util.List;

public interface SubCategoryMediaMapperDao extends GenericDao<SubCategoryMediaMapper, Long> {

    List<SubCategoryMediaMapper> findBySubCategoryId(Long subCategoryId);

    boolean existsBySubCategoryIdAndMediaId(Long subCategoryId, Long mediaId);

    SubCategoryMediaMapper findBySubCategoryIdAndMediaId(Long subCategoryId, Long mediaId);

    SubCategoryMediaMapper findPrimaryBySubCategoryId(Long subCategoryId);

    List<SubCategoryMediaMapper> findPrimaryBySubCategoryIds(List<Long> subCategoryIds);

    List<SubCategoryMediaMapper> findBySubCategoryIds(List<Long> subCategoryIds);
}
