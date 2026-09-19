package com.forvmom.core.services;

import com.forvmom.common.dto.request.*;
import com.forvmom.common.dto.response.*;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.ExperienceBeanMapper;
import com.forvmom.core.mapper.LocationBeanMapper;
import com.forvmom.data.dao.*;
import com.forvmom.data.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA-backed implementation of {@link LocationService}.
 *
 * <p>
 * Every operation is transactional; queries use {@code readOnly = true}. Read
 * paths return an empty list when nothing matches, except {@code getByCity},
 * which raises {@link ResourceNotFoundException} instead.
 *
 * <p>
 * Experience attachments are cache-coupled: {@code attachToExperience},
 * {@code updateExperienceAttachment} and {@code toggleExperienceAttachmentActive}
 * call {@code CatalogCacheService#warmLocationCache} inline, and
 * {@code detachFromExperience} calls {@code CatalogCacheService#evictLocation}.
 * This is done synchronously inside the write transaction so the asynchronous
 * booking enrichment path always reads a fresh Redis snapshot. The
 * category-location and sub-category-location attachments are not part of that
 * snapshot and therefore do not touch the cache.
 */
@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private PincodeDao pincodeDao;

    @Autowired
    private ExperienceLocationMapperDao locationMapperDao;

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private CatalogCacheService catalogCacheService;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private CategoryLocationMapperDao categoryLocationMapperDao;

    @Autowired
    private SubCategoryDao subCategoryDao;

    @Autowired
    private SubCategoryLocationMapperDao subCategoryLocationMapperDao;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Rejects duplicates by name before persisting.
     *
     * @param requestDto the location attributes
     * @return the created location
     * @throws IllegalArgumentException if a location with the same name exists
     */
    @Override
    @Transactional
    public LocationResponseDto createLocation(LocationRequestDto requestDto) {
        if (locationDao.existsByName(requestDto.getName())) {
            throw new IllegalArgumentException("Location with name '" + requestDto.getName() + "' already exists");
        }

        Location location = new Location();
        LocationBeanMapper.mapDtoToEntity(requestDto, location);
        Location saved = locationDao.save(location);
        return LocationBeanMapper.mapEntityToDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The uniqueness check is only performed when the name actually changes, so
     * re-saving a location under its own name is allowed.
     *
     * @param id         the location identifier
     * @param requestDto the new location attributes
     * @return the updated location
     * @throws ResourceNotFoundException if no location exists with the given id
     * @throws IllegalArgumentException  if the new name is taken by another
     *                                   location
     */
    @Override
    @Transactional
    public LocationResponseDto updateLocation(Long id, LocationRequestDto requestDto) {
        Location existing = locationDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Location not found with id " + id);
        }

        if (!existing.getName().equals(requestDto.getName()) && locationDao.existsByName(requestDto.getName())) {
            throw new IllegalArgumentException("Location with name '" + requestDto.getName() + "' already exists");
        }

        LocationBeanMapper.mapDtoToEntity(requestDto, existing);
        Location updated = locationDao.update(existing);
        return LocationBeanMapper.mapEntityToDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Uses the fetch-joined query so the pincodes are loaded with the location.
     *
     * @param id the location identifier
     * @return the location with its pincodes
     * @throws ResourceNotFoundException if no location exists with the given id
     */
    @Override
    @Transactional(readOnly = true)
    public LocationResponseDto getById(Long id) {
        Location location = locationDao.findByIdWithPincodes(id);
        if (location == null) {
            throw new ResourceNotFoundException("Location not found with id " + id);
        }
        return LocationBeanMapper.mapEntityToDto(location);
    }

    /**
     * {@inheritDoc}
     *
     * @return all locations, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<LocationResponseDto> getAll() {
        List<Location> locations = locationDao.findAll();
        if (locations == null || locations.isEmpty())
            return new ArrayList<>();
        return locations.stream().map(LocationBeanMapper::mapEntityToDto).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @return the active locations, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<LocationResponseDto> getAllActive() {
        List<Location> locations = locationDao.findAllActive();
        if (locations == null || locations.isEmpty())
            return new ArrayList<>();
        return locations.stream().map(LocationBeanMapper::mapEntityToDto).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Unlike the other list methods, an empty result is treated as an error here.
     *
     * @param city the city name to filter on
     * @return the locations in that city
     * @throws ResourceNotFoundException if the city has no locations
     */
    @Override
    @Transactional(readOnly = true)
    public List<LocationResponseDto> getByCity(String city) {
        List<Location> locations = locationDao.findByCity(city);
        if (locations == null || locations.isEmpty()) {
            throw new ResourceNotFoundException("No locations found for city: " + city);
        }
        return locations.stream().map(LocationBeanMapper::mapEntityToDto).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param id the location identifier
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no location exists with the given id
     */
    @Override
    @Transactional
    public boolean deleteLocation(Long id) {
        Location existing = locationDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Location not found with id " + id);
        }
        locationDao.delete(existing);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param id the location identifier
     * @throws ResourceNotFoundException if no location exists with the given id
     */
    @Override
    @Transactional
    public void toggleActive(Long id) {
        Location existing = locationDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Location not found with id " + id);
        }
        existing.setActive(!existing.isActive());
        locationDao.update(existing);
    }

    // ─── Pincode operations ───────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * The pincode code only has to be unique within its own location, so the same
     * code may be registered against different locations.
     *
     * @param requestDto the pincode attributes, including the owning location id
     * @return the created pincode
     * @throws ResourceNotFoundException if the referenced location does not exist
     * @throws IllegalArgumentException  if the code is already registered for that
     *                                   location
     */
    @Override
    @Transactional
    public PincodeResponseDto addPincode(PincodeRequestDto requestDto) {
        Location location = locationDao.findById(requestDto.getLocationId());
        if (location == null) {
            throw new ResourceNotFoundException("Location not found with id " + requestDto.getLocationId());
        }

        if (pincodeDao.existsByPincodeCodeAndLocationId(requestDto.getPincodeCode(), requestDto.getLocationId())) {
            throw new IllegalArgumentException(
                    "Pincode '" + requestDto.getPincodeCode() + "' already exists for this location");
        }

        Pincode pincode = new Pincode();
        pincode.setName(requestDto.getName());
        pincode.setPincodeCode(requestDto.getPincodeCode());
        pincode.setAreaName(requestDto.getAreaName());
        pincode.setLatitude(requestDto.getLatitude());
        pincode.setLongitude(requestDto.getLongitude());
        if (requestDto.getIsActive() != null)
            pincode.setActive(requestDto.getIsActive());
        pincode.setLocation(location);

        Pincode saved = pincodeDao.save(pincode);
        return LocationBeanMapper.mapPincodeToDto(saved, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The pincode is loaded together with its location so the uniqueness check can
     * be scoped to that location. The location itself cannot be reassigned here.
     *
     * @param pincodeId  the pincode identifier
     * @param requestDto the new pincode attributes
     * @return the updated pincode
     * @throws ResourceNotFoundException if no pincode exists with the given id
     * @throws IllegalArgumentException  if the new code is already used within the
     *                                   same location
     */
    @Override
    @Transactional
    public PincodeResponseDto updatePincode(Long pincodeId, PincodeRequestDto requestDto) {
        Pincode existing = pincodeDao.findByIdWithLocation(pincodeId);
        if (existing == null) {
            throw new ResourceNotFoundException("Pincode not found with id " + pincodeId);
        }

        // If pincode code is changing, check uniqueness within the same location
        if (!existing.getPincodeCode().equals(requestDto.getPincodeCode()) &&
                pincodeDao.existsByPincodeCodeAndLocationId(requestDto.getPincodeCode(),
                        existing.getLocation().getId())) {
            throw new IllegalArgumentException(
                    "Pincode '" + requestDto.getPincodeCode() + "' already exists for this location");
        }

        existing.setName(requestDto.getName());
        existing.setPincodeCode(requestDto.getPincodeCode());
        existing.setAreaName(requestDto.getAreaName());
        existing.setLatitude(requestDto.getLatitude());
        existing.setLongitude(requestDto.getLongitude());
        if (requestDto.getIsActive() != null)
            existing.setActive(requestDto.getIsActive());

        Pincode updated = pincodeDao.update(existing);
        return LocationBeanMapper.mapPincodeToDto(updated, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param locationId the location identifier
     * @return the pincodes of that location, or an empty list when none exist
     * @throws ResourceNotFoundException if no location exists with the given id
     */
    @Override
    @Transactional(readOnly = true)
    public List<PincodeResponseDto> getPincodesByLocation(Long locationId) {
        Location location = locationDao.findById(locationId);
        if (location == null) {
            throw new ResourceNotFoundException("Location not found with id " + locationId);
        }
        List<Pincode> pincodes = pincodeDao.findByLocationIdWithLocation(locationId);
        return pincodes.stream()
                .map(p -> LocationBeanMapper.mapPincodeToDto(p, true))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * An unknown code is reported as "not serviceable".
     *
     * @param pincodeCode the pincode code entered by the customer
     * @return the matching pincode together with its location
     * @throws ResourceNotFoundException if the code is not registered
     */
    @Override
    @Transactional(readOnly = true)
    public PincodeResponseDto checkPincode(String pincodeCode) {
        Pincode pincode = pincodeDao.findByPincodeCode(pincodeCode);
        if (pincode == null) {
            throw new ResourceNotFoundException("Pincode '" + pincodeCode + "' is not serviceable");
        }
        return LocationBeanMapper.mapPincodeToDto(pincode, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param pincodeId the pincode identifier
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no pincode exists with the given id
     */
    @Override
    @Transactional
    public boolean deletePincode(Long pincodeId) {
        Pincode existing = pincodeDao.findById(pincodeId);
        if (existing == null) {
            throw new ResourceNotFoundException("Pincode not found with id " + pincodeId);
        }
        pincodeDao.delete(existing);
        return true;
    }

    // ── Experience Association ────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Creates the junction row, wires it to the experience through the
     * bidirectional helper, and then warms the Redis location snapshot inline so
     * the asynchronous booking enrichment path sees the new mapping immediately.
     *
     * @param locationId   the location identifier
     * @param experienceId the experience identifier
     * @param requestDto   price override, validity window and active flag; the
     *                     active flag defaults to {@code true} when absent
     * @return the created experience-location mapping
     * @throws IllegalStateException     if the pair is already attached
     * @throws ResourceNotFoundException if the location or experience is unknown
     */
    @Override
    @Transactional
    public ExperienceLocationResponseDto attachToExperience(Long locationId, Long experienceId,
            ExperienceLocationAttachRequestDto requestDto) {

        if (locationMapperDao.existsByExperienceIdAndLocationId(experienceId, locationId)) {
            throw new IllegalStateException(
                    "Location " + locationId + " is already attached to experience " + experienceId);
        }

        Location location = locationDao.findById(locationId);
        if (location == null)
            throw new ResourceNotFoundException("Location not found: " + locationId);

        Experience experience = experienceDao.findById(experienceId);
        if (experience == null)
            throw new ResourceNotFoundException("Experience not found: " + experienceId);

        ExperienceLocationMapper mapper = new ExperienceLocationMapper();
        mapper.setLocation(location);
        mapper.setPriceOverride(requestDto.getPriceOverride());
        mapper.setValidFrom(requestDto.getValidFrom());
        mapper.setValidTo(requestDto.getValidTo());
        mapper.setIsActive(requestDto.getIsActive() != null ? requestDto.getIsActive() : true);

        // Bidirectional helper wires experience → mapper back-reference
        experience.addLocationMapper(mapper);

        ExperienceLocationMapper savedMapper = locationMapperDao.save(mapper);
        catalogCacheService.warmLocationCache(savedMapper);

        return ExperienceBeanMapper.mapLocationMapperToDto(savedMapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The cache entry is evicted right after the junction row is removed, so a
     * stale snapshot cannot be served for an experience-location pair that no
     * longer exists.
     *
     * @param locationId   the location identifier
     * @param experienceId the experience identifier
     * @throws ResourceNotFoundException if the pair is not attached
     */
    @Override
    @Transactional
    public void detachFromExperience(Long locationId, Long experienceId) {
        ExperienceLocationMapper mapper = locationMapperDao.findByExperienceIdAndLocationId(experienceId, locationId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Location " + locationId + " is not attached to experience " + experienceId);
        }
        locationMapperDao.delete(mapper);
        catalogCacheService.evictLocation(experienceId, locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @param locationId the location identifier
     * @return the experience mappings for that location, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceLocationResponseDto> getExperiencesForLocation(Long locationId) {
        // Re-uses DAO query keyed by experienceId — but we want by locationId.
        // The ExperienceLocationMapperDao.findByExperienceId covers experience-side
        // listing.
        // For location-side we query all mappers and filter; a dedicated DAO method can
        // be added later.
        List<ExperienceLocationMapper> mappers = locationMapperDao.findByLocationId(locationId);
        return ExperienceBeanMapper.mapLocationMappers(new ArrayList<>(mappers));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Price override and validity window are always overwritten with the request
     * values, while the active flag is only changed when supplied. The refreshed
     * mapping is written back into the Redis snapshot inline.
     *
     * @param locationId   the location identifier
     * @param experienceId the experience identifier
     * @param requestDto   the new price override, validity window and active flag
     * @return the updated mapping
     * @throws ResourceNotFoundException if the pair is not attached
     */
    @Override
    @Transactional
    public ExperienceLocationResponseDto updateExperienceAttachment(Long locationId, Long experienceId,
            ExperienceLocationAttachRequestDto requestDto) {
        ExperienceLocationMapper mapper = locationMapperDao.findByExperienceIdAndLocationId(experienceId, locationId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Location " + locationId + " is not attached to experience " + experienceId);
        }
        mapper.setPriceOverride(requestDto.getPriceOverride());
        mapper.setValidFrom(requestDto.getValidFrom());
        mapper.setValidTo(requestDto.getValidTo());
        if (requestDto.getIsActive() != null)
            mapper.setIsActive(requestDto.getIsActive());

        ExperienceLocationMapper updated = locationMapperDao.update(mapper);
        catalogCacheService.warmLocationCache(updated);

        return ExperienceBeanMapper.mapLocationMapperToDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} active flag is treated as inactive, so toggling turns it on.
     * The snapshot is re-warmed with the new state.
     *
     * @param mapperId the experience-location mapping identifier
     * @throws ResourceNotFoundException if no such mapping exists
     */
    @Override
    @Transactional
    public void toggleExperienceAttachmentActive(Long mapperId) {
        ExperienceLocationMapper mapper = locationMapperDao.findById(mapperId);
        if (mapper == null)
            throw new ResourceNotFoundException("Location mapping not found: " + mapperId);
        mapper.setIsActive(!Boolean.TRUE.equals(mapper.getIsActive()));
        ExperienceLocationMapper updated = locationMapperDao.update(mapper);
        catalogCacheService.warmLocationCache(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Display order defaults to {@code 0} and the active flag to {@code true} when
     * the request leaves them unset.
     *
     * @param locationId the location identifier
     * @param categoryId the category identifier
     * @param requestDto display order and active flag for the mapping
     * @return the created category-location mapping
     * @throws IllegalStateException     if the category is already attached to the
     *                                   location
     * @throws ResourceNotFoundException if the location or category is unknown
     */
    @Override
    @Transactional
    public CategoryLocationResponseDto attachCategoryToLocation(Long locationId, Long categoryId,
                                                                CategoryLocationAttachRequestDto requestDto) {
        if (categoryLocationMapperDao.existsByCategoryIdAndLocationId(categoryId, locationId)) {
            throw new IllegalStateException("Category " + categoryId + " is already attached to location " + locationId);
        }

        Location location = locationDao.findById(locationId);
        if (location == null) throw new ResourceNotFoundException("Location not found: " + locationId);

        Category category = categoryDao.findById(categoryId);
        if (category == null) throw new ResourceNotFoundException("Category not found: " + categoryId);

        CategoryLocationMapper mapper = new CategoryLocationMapper();
        mapper.setLocation(location);
        mapper.setCategory(category);
        mapper.setDisplayOrder(requestDto.getDisplayOrder() != null ? requestDto.getDisplayOrder() : 0);
        mapper.setActive(requestDto.getActive() != null ? requestDto.getActive() : true);

        CategoryLocationMapper saved = categoryLocationMapperDao.save(mapper);
        return LocationBeanMapper.mapCategoryLocationToDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param locationId the location identifier
     * @param categoryId the category identifier
     * @throws ResourceNotFoundException if the category is not attached to the
     *                                   location
     */
    @Override
    @Transactional
    public void detachCategoryFromLocation(Long locationId, Long categoryId) {
        CategoryLocationMapper mapper = categoryLocationMapperDao.findByCategoryIdAndLocationId(categoryId, locationId);
        if (mapper == null) {
            throw new ResourceNotFoundException("Category " + categoryId + " is not attached to location " + locationId);
        }
        categoryLocationMapperDao.delete(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * @param locationId the location identifier
     * @return the category mappings for that location, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryLocationResponseDto> getCategoriesForLocation(Long locationId) {
        List<CategoryLocationMapper> mappers = categoryLocationMapperDao.findByLocationId(locationId);
        return mappers.stream().map(LocationBeanMapper::mapCategoryLocationToDto).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Only the fields present in the request are applied, so a partial payload
     * leaves the remaining mapping attributes untouched.
     *
     * @param locationId the location identifier
     * @param categoryId the category identifier
     * @param requestDto the fields to update
     * @return the updated mapping
     * @throws ResourceNotFoundException if the category is not attached to the
     *                                   location
     */
    @Override
    @Transactional
    public CategoryLocationResponseDto updateCategoryAttachment(Long locationId, Long categoryId,
                                                                CategoryLocationAttachRequestDto requestDto) {
        CategoryLocationMapper mapper = categoryLocationMapperDao.findByCategoryIdAndLocationId(categoryId, locationId);
        if (mapper == null) {
            throw new ResourceNotFoundException("Category " + categoryId + " is not attached to location " + locationId);
        }
        if (requestDto.getDisplayOrder() != null) {
            mapper.setDisplayOrder(requestDto.getDisplayOrder());
        }
        if (requestDto.getActive() != null) {
            mapper.setActive(requestDto.getActive());
        }
        CategoryLocationMapper updated = categoryLocationMapperDao.update(mapper);
        return LocationBeanMapper.mapCategoryLocationToDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} active flag is treated as inactive, so toggling turns it on.
     *
     * @param mapperId the category-location mapping identifier
     * @throws ResourceNotFoundException if no such mapping exists
     */
    @Override
    @Transactional
    public void toggleCategoryAttachmentActive(Long mapperId) {
        CategoryLocationMapper mapper = categoryLocationMapperDao.findById(mapperId);
        if (mapper == null) throw new ResourceNotFoundException("Category-Location mapping not found: " + mapperId);
        mapper.setActive(!Boolean.TRUE.equals(mapper.getActive()));
        categoryLocationMapperDao.update(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Projects each active mapping into a flat DTO of category id, name, slug and
     * the mapping's display order.
     *
     * @param locationId the location identifier
     * @return the active categories for that location, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryByLocationDto> getActiveCategoriesByLocation(Long locationId) {
        List<CategoryLocationMapper> mappers = categoryLocationMapperDao.findActiveByLocationId(locationId);
        return mappers.stream()
                .map(m -> new CategoryByLocationDto(
                        m.getCategory().getId(),
                        m.getCategory().getName(),
                        m.getCategory().getSlug(),
                        m.getDisplayOrder()))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Display order defaults to {@code 0} and the active flag to {@code true} when
     * the request leaves them unset.
     *
     * @param locationId    the location identifier
     * @param subCategoryId the sub-category identifier
     * @param requestDto    display order and active flag for the mapping
     * @return the created sub-category-location mapping
     * @throws IllegalStateException     if the sub-category is already attached to
     *                                   the location
     * @throws ResourceNotFoundException if the location or sub-category is unknown
     */
    @Override
    @Transactional
    public SubCategoryLocationResponseDto attachSubCategoryToLocation(Long locationId, Long subCategoryId,
                                                                      SubCategoryLocationAttachRequestDto requestDto) {
        if (subCategoryLocationMapperDao.existsBySubCategoryIdAndLocationId(subCategoryId, locationId)) {
            throw new IllegalStateException("SubCategory " + subCategoryId + " is already attached to location " + locationId);
        }

        Location location = locationDao.findById(locationId);
        if (location == null) throw new ResourceNotFoundException("Location not found: " + locationId);

        SubCategory subCategory = subCategoryDao.findById(subCategoryId);
        if (subCategory == null) throw new ResourceNotFoundException("SubCategory not found: " + subCategoryId);

        SubCategoryLocationMapper mapper = new SubCategoryLocationMapper();
        mapper.setLocation(location);
        mapper.setSubCategory(subCategory);
        mapper.setDisplayOrder(requestDto.getDisplayOrder() != null ? requestDto.getDisplayOrder() : 0);
        mapper.setActive(requestDto.getActive() != null ? requestDto.getActive() : true);

        SubCategoryLocationMapper saved = subCategoryLocationMapperDao.save(mapper);
        return LocationBeanMapper.mapSubCategoryLocationToDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param locationId    the location identifier
     * @param subCategoryId the sub-category identifier
     * @throws ResourceNotFoundException if the sub-category is not attached to the
     *                                   location
     */
    @Override
    @Transactional
    public void detachSubCategoryFromLocation(Long locationId, Long subCategoryId) {
        SubCategoryLocationMapper mapper = subCategoryLocationMapperDao.findBySubCategoryIdAndLocationId(subCategoryId, locationId);
        if (mapper == null) {
            throw new ResourceNotFoundException("SubCategory " + subCategoryId + " is not attached to location " + locationId);
        }
        subCategoryLocationMapperDao.delete(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * @param locationId the location identifier
     * @return the sub-category mappings for that location, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryLocationResponseDto> getSubCategoriesForLocation(Long locationId) {
        List<SubCategoryLocationMapper> mappers = subCategoryLocationMapperDao.findByLocationId(locationId);
        return mappers.stream().map(LocationBeanMapper::mapSubCategoryLocationToDto).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Only the fields present in the request are applied.
     *
     * @param locationId    the location identifier
     * @param subCategoryId the sub-category identifier
     * @param requestDto    the fields to update
     * @return the updated mapping
     * @throws ResourceNotFoundException if the sub-category is not attached to the
     *                                   location
     */
    @Override
    @Transactional
    public SubCategoryLocationResponseDto updateSubCategoryAttachment(Long locationId, Long subCategoryId,
                                                                      SubCategoryLocationAttachRequestDto requestDto) {
        SubCategoryLocationMapper mapper = subCategoryLocationMapperDao.findBySubCategoryIdAndLocationId(subCategoryId, locationId);
        if (mapper == null) {
            throw new ResourceNotFoundException("SubCategory " + subCategoryId + " is not attached to location " + locationId);
        }
        if (requestDto.getDisplayOrder() != null) {
            mapper.setDisplayOrder(requestDto.getDisplayOrder());
        }
        if (requestDto.getActive() != null) {
            mapper.setActive(requestDto.getActive());
        }
        SubCategoryLocationMapper updated = subCategoryLocationMapperDao.update(mapper);
        return LocationBeanMapper.mapSubCategoryLocationToDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} active flag is treated as inactive, so toggling turns it on.
     *
     * @param mapperId the sub-category-location mapping identifier
     * @throws ResourceNotFoundException if no such mapping exists
     */
    @Override
    @Transactional
    public void toggleSubCategoryAttachmentActive(Long mapperId) {
        SubCategoryLocationMapper mapper = subCategoryLocationMapperDao.findById(mapperId);
        if (mapper == null) throw new ResourceNotFoundException("SubCategory-Location mapping not found: " + mapperId);
        mapper.setActive(!Boolean.TRUE.equals(mapper.getActive()));
        subCategoryLocationMapperDao.update(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Projects each active mapping into a flat DTO carrying the sub-category id,
     * name and slug, its parent category id and name, and the mapping's display
     * order.
     *
     * @param locationId the location identifier
     * @return the active sub-categories for that location, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryByLocationDto> getActiveSubCategoriesByLocation(Long locationId) {
        List<SubCategoryLocationMapper> mappers = subCategoryLocationMapperDao.findActiveByLocationId(locationId);
        return mappers.stream()
                .map(m -> new SubCategoryByLocationDto(
                        m.getSubCategory().getId(),
                        m.getSubCategory().getName(),
                        m.getSubCategory().getSlug(),
                        m.getSubCategory().getCategory().getId(),
                        m.getSubCategory().getCategory().getName(),
                        m.getDisplayOrder()))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Same projection as {@link #getActiveSubCategoriesByLocation(Long)} but
     * restricted to a single parent category by the DAO query.
     *
     * @param locationId the location identifier
     * @param categoryId the parent category identifier
     * @return the matching active sub-categories, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryByLocationDto> getActiveSubCategoriesByLocationAndCategory(Long locationId, Long categoryId) {
        List<SubCategoryLocationMapper> mappers = subCategoryLocationMapperDao.findActiveByLocationIdAndCategoryId(locationId, categoryId);
        return mappers.stream()
                .map(m -> new SubCategoryByLocationDto(
                        m.getSubCategory().getId(),
                        m.getSubCategory().getName(),
                        m.getSubCategory().getSlug(),
                        m.getSubCategory().getCategory().getId(),
                        m.getSubCategory().getCategory().getName(),
                        m.getDisplayOrder()))
                .collect(Collectors.toList());
    }

}
