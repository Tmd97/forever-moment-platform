package com.forvmom.core.services;

import com.forvmom.common.dto.request.ReorderRequestDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.common.dto.request.CategoryRequestDto;
import com.forvmom.common.dto.response.CategoryResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.core.mapper.CategoryBeanMapper;
import com.forvmom.data.dao.CategoryDao;
import com.forvmom.data.entities.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that owns the top-level catalog {@link Category} entities.
 *
 * <p>
 * Category names must be unique. New categories are appended to the end of the
 * display order: the service asks {@code ReorderingService} for the current
 * maximum order value and stores {@code max + 1}, which keeps the dense 1-based
 * ordering maintained by {@code ReorderingService} intact. Repositioning is
 * delegated back to that service through {@link #reorderCategories(Long, Long)}.
 *
 * <p>
 * The class is annotated {@code @Transactional} at type level, so all public
 * methods participate in a transaction; read methods are marked
 * {@code readOnly = true} and use fetch-joined queries that load sub-categories
 * along with the category.
 */
@Service
@Transactional
public class CategoryService extends ReorderingService {

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private ReorderingService reorderingService;

    // TODO remove from request the display Order, as backend have done set and
    // return
    /**
     * Creates a category and appends it to the end of the display order.
     *
     * @param categoryRequestDto the category to create; its name must be unique
     * @return the persisted category as a response DTO
     * @throws IllegalArgumentException if a category with the same name exists
     */
    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        if (categoryDao.existsByName(categoryRequestDto.getName())) {
            throw new IllegalArgumentException(
                    "Category with name '" + categoryRequestDto.getName() + "' already exists");
        }
        Category category = new Category();
        CategoryBeanMapper.mapDtoToEntity(categoryRequestDto, category);
        Long max = reorderingService.getMaxOrder(Category.class);
        category.setDisplayOrder(max + 1);
        Category res = categoryDao.save(category);
        return CategoryBeanMapper.mapEntityToDto(res);
    }

    /**
     * Applies the updatable fields of the request to an existing category. The
     * display order is not recalculated here; use
     * {@link #reorderCategories(Long, Long)} for that.
     *
     * @param id                 the identifier of the category to update
     * @param categoryRequestDto the new field values
     * @return the updated category as a response DTO
     * @throws ResourceNotFoundException if no category exists with the given id
     */
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto categoryRequestDto) {

        Category existing = categoryDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Category not found with given Id " + id);
        }
        // map only updatable fields
        CategoryBeanMapper.mapDtoToEntity(categoryRequestDto, existing);
        Category res = categoryDao.update(existing);
        return CategoryBeanMapper.mapEntityToDto(res);

    }

    /**
     * Loads a category together with its sub-categories in a single query.
     *
     * @param id the category identifier
     * @return the category as a response DTO
     * @throws ResourceNotFoundException if no category exists with the given id
     */
    @Transactional(readOnly = true)
    public CategoryResponseDto getById(Long id) {
        // Use optimized query to fetch Category + SubCategories
        Category category = categoryDao.findByIdWithSubCategories(id);
        if (category == null) {
            throw new ResourceNotFoundException("Category with given Id " + id + " is not exist");
        }
        return CategoryBeanMapper.mapEntityToDto(category);
    }

    /**
     * Lists every category with its sub-categories fetch-joined.
     *
     * @return all categories, or an empty list when none exist
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAll() {
        // Use optimized query to fetch Categories + SubCategories
        List<Category> categories = categoryDao.findAllWithSubCategories();
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        } else {
            return categories.stream()
                    .map(CategoryBeanMapper::mapEntityToDto)
                    .toList();
        }
    }

    /**
     * Deletes a category through the DAO's delete semantics.
     *
     * @param id the identifier of the category to delete
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no category exists with the given id
     */
    @Transactional
    public boolean deleteCategory(Long id) {
        Category existing = categoryDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("No such category for given Id exist " + id);
        }
        categoryDao.delete(existing);
        return true;
    }

    /**
     * Moves a category to a new position, delegating to {@code ReorderingService}
     * which shifts the surrounding rows so the 1-based ordering stays dense.
     *
     * @param id          the identifier of the category to move
     * @param newPosition the target 1-based display position
     */
    public void reorderCategories(Long id, Long newPosition) {
        reorderingService.reorderItems(id, newPosition, Category.class);

    }
}