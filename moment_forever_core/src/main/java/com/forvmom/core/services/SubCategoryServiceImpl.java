package com.forvmom.core.services;

import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.common.dto.request.SubCategoryRequestDto;
import com.forvmom.common.dto.response.SubCategoryResponseDto;
import com.forvmom.core.mapper.SubCategoryBeanMapper;
import com.forvmom.data.dao.CategoryDao;
import com.forvmom.data.dao.SubCategoryDao;
import com.forvmom.data.entities.Category;
import com.forvmom.data.entities.SubCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA-backed implementation of {@link SubCategoryService}.
 *
 * <p>
 * Names and slugs are globally unique across sub-categories, not just within a
 * parent category. New rows are appended to the end of the display order by
 * asking {@code ReorderingService} for the current maximum and storing
 * {@code max + 1}, keeping the dense 1-based ordering intact; existing rows keep
 * their order on update.
 *
 * <p>
 * All methods are transactional, with read methods marked
 * {@code readOnly = true} and using fetch-joined queries so the parent category
 * is available to the mapper.
 */
@Service
public class SubCategoryServiceImpl implements SubCategoryService {

    @Autowired
    private SubCategoryDao subCategoryDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private ReorderingService reorderingService;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Validates that neither the name nor the slug is taken, resolves the parent
     * category, then appends the new row to the end of the display order.
     *
     * @param requestDto the sub-category attributes, including the parent category
     *                   id
     * @return the created sub-category
     * @throws IllegalArgumentException  if the name or slug already exists
     * @throws ResourceNotFoundException if the parent category does not exist
     */
    @Override
    @Transactional
    public SubCategoryResponseDto createSubCategory(SubCategoryRequestDto requestDto) {
        // Check if SubCategory with same name already exists
        if (subCategoryDao.existsByName(requestDto.getName())) {
            throw new IllegalArgumentException("SubCategory with name '" + requestDto.getName() + "' already exists");
        }

        if (subCategoryDao.existsBySlug(requestDto.getSlug())) {
            throw new IllegalArgumentException("SubCategory with slug '" + requestDto.getSlug() + "' already exists");
        }

        Category category = categoryDao.findById(requestDto.getCategoryId());
        if (category == null) {
            throw new ResourceNotFoundException("No Category exist with id " + requestDto.getCategoryId());
        }

        // Link SubCategory to Category and save
        SubCategory subCategory = new SubCategory();
        SubCategoryBeanMapper.mapDtoToEntity(requestDto, subCategory);
        subCategory.setCategory(category);
        Long max = reorderingService.getMaxOrder(SubCategory.class);
        subCategory.setDisplayOrder(max + 1);
        SubCategory saved = subCategoryDao.save(subCategory);

        SubCategoryResponseDto responseDto = SubCategoryBeanMapper.mapEntityToDto(saved);
        return responseDto;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Name and slug uniqueness are only re-checked when the value actually
     * changes; the slug check is additionally skipped when the stored slug is
     * {@code null}. The parent category is not changed here, see
     * {@link #associateSubCategoryToCategory(Long, Long)}.
     *
     * @param id         the sub-category identifier
     * @param requestDto the new attributes
     * @return the updated sub-category
     * @throws ResourceNotFoundException if no sub-category exists with the given
     *                                   id
     * @throws IllegalArgumentException  if the new name or slug is already taken
     */
    @Override
    @Transactional
    public SubCategoryResponseDto updateSubCategory(Long id, SubCategoryRequestDto requestDto) {
        SubCategory existing = subCategoryDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("SubCategory not found with id " + id);
        }

        // If name is being changed, check for duplicates
        if (!existing.getName().equals(requestDto.getName()) &&
                subCategoryDao.existsByName(requestDto.getName())) {
            throw new IllegalArgumentException("SubCategory with name '" + requestDto.getName() + "' already exists");
        }

        // If slug is being changed, check for duplicates
        if (existing.getSlug() != null && (!existing.getSlug().equals(requestDto.getSlug()) &&
                subCategoryDao.existsBySlug(requestDto.getSlug()))) {
            throw new IllegalArgumentException("SubCategory with slug '" + requestDto.getSlug() + "' already exists");
        }

        SubCategoryBeanMapper.mapDtoToEntity(requestDto, existing);
        SubCategory updated = subCategoryDao.update(existing);
        return SubCategoryBeanMapper.mapEntityToDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * @param id the sub-category identifier
     * @return the sub-category with its parent category
     * @throws ResourceNotFoundException if no sub-category exists with the given
     *                                   id
     */
    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponseDto getById(Long id) {
        SubCategory subCategory = subCategoryDao.findByIdWithCategory(id);
        if (subCategory == null) {
            throw new ResourceNotFoundException("SubCategory with id " + id + " does not exist");
        }
        return SubCategoryBeanMapper.mapEntityToDto(subCategory);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The DAO returns a list; the first match is used since slugs are unique.
     *
     * @param slug the sub-category slug
     * @return the matching sub-category with its parent category
     * @throws ResourceNotFoundException if no sub-category has that slug
     */
    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponseDto getBySlug(String slug) {
        List<SubCategory> subCategories = subCategoryDao.findBySlugWithCategory(slug);
        if (subCategories.isEmpty()) {
            throw new ResourceNotFoundException("SubCategory with slug '" + slug + "' does not exist");
        }
        return SubCategoryBeanMapper.mapEntityToDto(subCategories.get(0));
    }

    /**
     * {@inheritDoc}
     *
     * @return all sub-categories, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponseDto> getAll() {
        // Use optimized query to fetch SubCategories + Category
        List<SubCategory> subCategories = subCategoryDao.findAllWithCategory();
        if (subCategories == null || subCategories.isEmpty()) {
            return new ArrayList<>();
        } else {
            return subCategories.stream()
                    .map(SubCategoryBeanMapper::mapEntityToDto)
                    .toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Unlike {@link #getAll()}, an empty result is reported as an error.
     *
     * @param categoryId the parent category identifier
     * @return the sub-categories of that category
     * @throws ResourceNotFoundException if the category has no sub-categories
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponseDto> getByCategoryId(Long categoryId) {
        // Use optimized query to fetch SubCategories + Category
        List<SubCategory> subCategories = subCategoryDao.findByCategoryIdWithCategory(categoryId);
        if (subCategories == null || subCategories.isEmpty()) {
            throw new ResourceNotFoundException("No SubCategories found for category id " + categoryId);
        }
        return subCategories.stream()
                .map(SubCategoryBeanMapper::mapEntityToDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param id the sub-category identifier
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no sub-category exists with the given
     *                                   id
     */
    @Override
    @Transactional
    public boolean deleteSubCategory(Long id) {
        SubCategory existing = subCategoryDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("No SubCategory exists with id " + id);
        }
        subCategoryDao.delete(existing);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Both sides are resolved first so an unknown id fails before anything is
     * written. The display order is left untouched by the re-parenting.
     *
     * @param id         the sub-category identifier
     * @param categoryId the identifier of the new parent category
     * @throws ResourceNotFoundException if the sub-category or the category does
     *                                   not exist
     */
    @Override
    @Transactional
    public void associateSubCategoryToCategory(Long id, Long categoryId) {
        SubCategory subCategory = subCategoryDao.findById(id);
        if (subCategory == null) {
            throw new ResourceNotFoundException("No SubCategory exists with id " + id);
        }

        Category category = categoryDao.findById(categoryId);
        if (category == null) {
            throw new ResourceNotFoundException("No Category exists with id " + categoryId);
        }

        subCategory.setCategory(category);
        subCategoryDao.update(subCategory);

    }
}