package com.forvmom.core.services;

import com.forvmom.common.dto.request.SubCategoryRequestDto;
import com.forvmom.common.dto.response.SubCategoryResponseDto;

import java.util.List;

/**
 * Contract for managing catalog sub-categories, the second level of the
 * category tree that experiences hang off.
 *
 * <p>
 * A sub-category always belongs to exactly one parent category and is
 * identified publicly by its slug. Implementations enforce uniqueness of both
 * name and slug and maintain the sub-category display order.
 */
public interface SubCategoryService {

    /**
     * Creates a sub-category under an existing parent category.
     *
     * @param subCategoryDto the sub-category attributes, including the parent
     *                       category id
     * @return the created sub-category
     */
    SubCategoryResponseDto createSubCategory(SubCategoryRequestDto subCategoryDto);

    /**
     * Updates an existing sub-category's attributes.
     *
     * @param id             the sub-category identifier
     * @param subCategoryDto the new attributes
     * @return the updated sub-category
     */
    SubCategoryResponseDto updateSubCategory(Long id, SubCategoryRequestDto subCategoryDto);

    /**
     * Loads a sub-category by primary key, including its parent category.
     *
     * @param id the sub-category identifier
     * @return the matching sub-category
     */
    SubCategoryResponseDto getById(Long id);

    /**
     * Loads a sub-category by its public slug.
     *
     * @param slug the sub-category slug
     * @return the matching sub-category
     */
    SubCategoryResponseDto getBySlug(String slug);

    /**
     * Lists every sub-category with its parent category.
     *
     * @return all sub-categories, or an empty list when none exist
     */
    List<SubCategoryResponseDto> getAll();

    /**
     * Lists the sub-categories of a parent category.
     *
     * @param categoryId the parent category identifier
     * @return the sub-categories of that category
     */
    List<SubCategoryResponseDto> getByCategoryId(Long categoryId);

    /**
     * Deletes a sub-category.
     *
     * @param id the sub-category identifier
     * @return {@code true} when the delete was issued
     */
    boolean deleteSubCategory(Long id);

    /**
     * Re-parents a sub-category onto a different category.
     *
     * @param id         the sub-category identifier
     * @param categoryId the identifier of the new parent category
     */
   void associateSubCategoryToCategory(Long id, Long categoryId);
}