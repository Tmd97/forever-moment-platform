package com.forvmom.core.services;

import com.forvmom.common.dto.request.BulkAttachTimeSlotsRequestDto;
import com.forvmom.common.dto.request.BulkAttachTimeSlotsRequestDto.TimeSlotAttachItem;
import com.forvmom.common.dto.request.ExperienceTimeSlotAttachRequestDto;
import com.forvmom.common.dto.request.TimeSlotRequestDto;
import com.forvmom.common.dto.response.BulkAttachTimeSlotsResultDto;
import com.forvmom.common.dto.response.BulkAttachTimeSlotsResultDto.SkippedTimeSlotDto;
import com.forvmom.common.dto.response.BulkAttachTimeSlotsResultDto.SkippedTimeSlotDto.Reason;
import com.forvmom.common.dto.response.ExperienceTimeSlotResponseDto;
import com.forvmom.common.dto.response.TimeSlotResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.TimeSlotBeanMapper;
import com.forvmom.data.dao.*;

import com.forvmom.data.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA-backed implementation of {@link ExperienceTimeSlotService}.
 *
 * <p>
 * The service covers two concerns. First, CRUD over the master
 * {@link TimeSlot} catalog, where a slot is considered a duplicate when both its
 * label and its start/end times match an existing row. Second, the attachment of
 * those master slots to a specific experience-and-location pair through an
 * {@link ExperienceTimeSlotMapper} that hangs off the
 * {@link ExperienceLocationMapper} junction, which is what actually carries slot
 * capacity for bookings.
 *
 * <p>
 * Attachment writes call {@code CatalogCacheService} inline:
 * {@code attachTimeSlot}, {@code updateAttachment} and
 * {@code toggleAttachmentActive} warm the Redis slot snapshot, while
 * {@code detachTimeSlot} evicts it, so the asynchronous booking enrichment path
 * always resolves slots against fresh data. Master time-slot CRUD does not touch
 * the cache, since the snapshots are keyed by the attachment rows.
 */
@Service
public class ExperienceTimeSlotServiceImpl implements ExperienceTimeSlotService {

    @Autowired
    private TimeSlotDao timeSlotDao;

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private ExperienceTimeSlotMapperDao timeSlotMapperDao;

    @Autowired
    private ExperienceLocationMapperDao expLocationMapperDao;

    @Autowired
    private CatalogCacheService catalogCacheService;

    // ── Master TimeSlot CRUD ──────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Uniqueness is defined by the combination of label plus start and end time,
     * so the same label may be reused for a different time range.
     *
     * @param requestDto the slot label and its start/end times as strings
     * @return the created master time slot
     * @throws IllegalArgumentException if an identical label and time range
     *                                  already exists
     */
    @Override
    @Transactional
    public TimeSlotResponseDto createTimeSlot(TimeSlotRequestDto requestDto) {
        TimeSlot existing = timeSlotDao.findByLabelAndTimeRange(
                requestDto.getLabel(),
                TimeSlotBeanMapper.parseTime(requestDto.getStartTime()),
                TimeSlotBeanMapper.parseTime(requestDto.getEndTime()));
        if (existing != null) {
            throw new IllegalArgumentException("Time slot with same label and time range already exists");
        }
        TimeSlot entity = TimeSlotBeanMapper.mapDtoToEntity(requestDto);
        return TimeSlotBeanMapper.mapEntityToDto(timeSlotDao.save(entity));
    }

    /**
     * {@inheritDoc}
     *
     * @return all master time slots, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponseDto> getAllTimeSlots() {
        return timeSlotDao.findAll().stream()
                .map(TimeSlotBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param id the master time slot identifier
     * @return the matching time slot
     * @throws ResourceNotFoundException if no time slot exists with the given id
     */
    @Override
    @Transactional(readOnly = true)
    public TimeSlotResponseDto getTimeSlotById(Long id) {
        return TimeSlotBeanMapper.mapEntityToDto(findTimeSlotOrThrow(id));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Matches on a label substring rather than an exact label.
     *
     * @param label the label fragment to search for
     * @return the matching time slots, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponseDto> getTimeSlotsByLabel(String label) {
        return timeSlotDao.findByLabelContaining(label).stream()
                .map(TimeSlotBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The string bounds are parsed into times before the DAO query is issued.
     *
     * @param startTime the range start as a parsable time string
     * @param endTime   the range end as a parsable time string
     * @return the matching time slots, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponseDto> getTimeSlotsByTimeRange(String startTime, String endTime) {
        return timeSlotDao.findByTimeRange(
                TimeSlotBeanMapper.parseTime(startTime),
                TimeSlotBeanMapper.parseTime(endTime)).stream().map(TimeSlotBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A duplicate hit that resolves to the slot being edited is ignored, so a slot
     * can be saved without changing its label or times.
     *
     * @param id         the master time slot identifier
     * @param requestDto the new label and times
     * @return the updated time slot
     * @throws ResourceNotFoundException if no time slot exists with the given id
     * @throws IllegalArgumentException  if another slot already uses the same label
     *                                   and time range
     */
    @Override
    @Transactional
    public TimeSlotResponseDto updateTimeSlot(Long id, TimeSlotRequestDto requestDto) {
        TimeSlot entity = findTimeSlotOrThrow(id);

        TimeSlot dupe = timeSlotDao.findByLabelAndTimeRange(
                requestDto.getLabel(),
                TimeSlotBeanMapper.parseTime(requestDto.getStartTime()),
                TimeSlotBeanMapper.parseTime(requestDto.getEndTime()));
        if (dupe != null && !dupe.getId().equals(id)) {
            throw new IllegalArgumentException("Another time slot with same label and time range already exists");
        }

        TimeSlotBeanMapper.updateEntityFromDto(entity, requestDto);
        return TimeSlotBeanMapper.mapEntityToDto(timeSlotDao.update(entity));
    }

    /**
     * {@inheritDoc}
     *
     * @param id the master time slot identifier
     * @throws ResourceNotFoundException if no time slot exists with the given id
     */
    @Override
    @Transactional
    public void deleteTimeSlot(Long id) {
        TimeSlot entity = findTimeSlotOrThrow(id);
        timeSlotDao.delete(entity);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} active flag is treated as inactive, so toggling turns it on.
     *
     * @param id the master time slot identifier
     * @throws ResourceNotFoundException if no time slot exists with the given id
     */
    @Override
    @Transactional
    public void toggleTimeSlotActive(Long id) {
        TimeSlot entity = findTimeSlotOrThrow(id);
        entity.setIsActive(!Boolean.TRUE.equals(entity.getIsActive()));
        timeSlotDao.update(entity);
    }

    // ── Experience-Location Attachment ────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Resolves (or lazily creates) the experience-location junction row, then
     * attaches the master slot to it and warms the Redis slot snapshot inline so
     * the asynchronous booking enrichment path can price and validate the slot
     * immediately.
     *
     * @param experienceId the experience identifier
     * @param locationId   the location identifier
     * @param timeSlotId   the master time slot identifier
     * @param requestDto   attachment attributes such as capacity and active flag
     * @return the created experience-time-slot mapping
     * @throws ResourceNotFoundException if the experience, location or time slot
     *                                   does not exist
     * @throws IllegalStateException     if the slot is already attached to this
     *                                   experience-location pair
     */
    @Override
    @Transactional
    public ExperienceTimeSlotResponseDto attachTimeSlot(Long experienceId, Long locationId, Long timeSlotId,
            ExperienceTimeSlotAttachRequestDto requestDto) {

        // Load experience and location entities (needed by findOrCreate)
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null)
            throw new ResourceNotFoundException("Experience not found: " + experienceId);

        Location location = locationDao.findById(locationId);
        if (location == null)
            throw new ResourceNotFoundException("Location not found: " + locationId);
        // TODO: not a good design (have to think later and fix it)
        // Get or create the ExperienceLocationMapper — safe to call even if the
        // location was never attached before. The DAO flushes immediately after
        // creating a new row, so its ID is available for FK use in this transaction.
        ExperienceLocationMapper expLocation = expLocationMapperDao.findOrCreate(experience, location);

        if (timeSlotMapperDao.existsByExperienceLocationIdAndTimeSlotId(expLocation.getId(), timeSlotId)) {
            throw new IllegalStateException(
                    "TimeSlot " + timeSlotId + " is already attached to this experience-location.");
        }

        TimeSlot timeSlot = findTimeSlotOrThrow(timeSlotId);

        ExperienceTimeSlotMapper mapper = TimeSlotBeanMapper.mapDtoToMapperEntity(requestDto);
        mapper.setTimeSlot(timeSlot);
        mapper.setExperienceLocation(expLocation);

        ExperienceTimeSlotMapper savedMapper = timeSlotMapperDao.save(mapper);
        catalogCacheService.warmSlotCache(savedMapper);

        return TimeSlotBeanMapper.mapMapperEntityToDto(savedMapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Attaches each requested slot through {@link #attachTimeSlot}, collecting
     * failures instead of aborting: an already attached slot is reported as
     * {@code DUPLICATE} and a missing slot or experience-location as
     * {@code NOT_FOUND}. Since everything runs in one transaction, the skipped
     * items are simply omitted from the attached list.
     *
     * @param experienceId the experience identifier
     * @param locationId   the location identifier
     * @param requestDto   the batch of slots to attach
     * @return the attached mappings together with the skipped items and reasons
     */
    @Override
    @Transactional
    public BulkAttachTimeSlotsResultDto attachTimeSlots(Long experienceId, Long locationId,
            BulkAttachTimeSlotsRequestDto requestDto) {

        List<ExperienceTimeSlotResponseDto> attached = new ArrayList<>();
        List<SkippedTimeSlotDto> skipped = new ArrayList<>();

        for (TimeSlotAttachItem item : requestDto.getItems()) {
            Long tsId = item.getTimeSlotId();
            try {
                attached.add(attachTimeSlot(experienceId, locationId, tsId, item));
            } catch (IllegalStateException e) {
                // already attached
                skipped.add(new SkippedTimeSlotDto(tsId, Reason.DUPLICATE, e.getMessage()));
            } catch (ResourceNotFoundException e) {
                // master TimeSlot or experience-location not found
                skipped.add(new SkippedTimeSlotDto(tsId, Reason.NOT_FOUND, e.getMessage()));
            }
        }

        return new BulkAttachTimeSlotsResultDto(attached, skipped);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The mapping id is captured before the delete so the cache entry can still be
     * evicted afterwards.
     *
     * @param experienceId the experience identifier
     * @param locationId   the location identifier
     * @param timeSlotId   the master time slot identifier
     * @throws ResourceNotFoundException if the location is not attached to the
     *                                   experience, or the slot is not attached to
     *                                   that pair
     */
    @Override
    @Transactional
    public void detachTimeSlot(Long experienceId, Long locationId, Long timeSlotId) {
        ExperienceLocationMapper expLocation = resolveExpLocation(experienceId, locationId);

        ExperienceTimeSlotMapper mapper = timeSlotMapperDao.findByExperienceLocationIdAndTimeSlotId(expLocation.getId(),
                timeSlotId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "TimeSlot " + timeSlotId + " is not attached to this experience-location.");
        }
        Long mapperId = mapper.getId();
        timeSlotMapperDao.delete(mapper);
        catalogCacheService.evictSlot(mapperId);
    }

    /**
     * {@inheritDoc}
     *
     * @param experienceId the experience identifier
     * @param locationId   the location identifier
     * @return the slot mappings of that experience-location pair
     * @throws ResourceNotFoundException if the location is not attached to the
     *                                   experience
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceTimeSlotResponseDto> getTimeSlotsForExperienceLocation(
            Long experienceId, Long locationId) {
        ExperienceLocationMapper expLocation = resolveExpLocation(experienceId, locationId);
        return TimeSlotBeanMapper.mapMapperEntitiesToDto(
                timeSlotMapperDao.findByExperienceLocationId(expLocation.getId()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Applies the request to the existing mapping and re-warms the Redis slot
     * snapshot with the updated values.
     *
     * @param experienceId the experience identifier
     * @param locationId   the location identifier
     * @param timeSlotId   the master time slot identifier
     * @param requestDto   the attachment attributes to apply
     * @return the updated mapping
     * @throws ResourceNotFoundException if the location is not attached to the
     *                                   experience, or the slot is not attached to
     *                                   that pair
     */
    @Override
    @Transactional
    public ExperienceTimeSlotResponseDto updateAttachment(Long experienceId, Long locationId, Long timeSlotId,
            ExperienceTimeSlotAttachRequestDto requestDto) {
        ExperienceLocationMapper expLocation = resolveExpLocation(experienceId, locationId);

        ExperienceTimeSlotMapper mapper = timeSlotMapperDao.findByExperienceLocationIdAndTimeSlotId(expLocation.getId(),
                timeSlotId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "TimeSlot " + timeSlotId + " is not attached to this experience-location.");
        }
        TimeSlotBeanMapper.updateMapperEntityFromDto(mapper, requestDto);
        ExperienceTimeSlotMapper updated = timeSlotMapperDao.update(mapper);
        catalogCacheService.warmSlotCache(updated);
        return TimeSlotBeanMapper.mapMapperEntityToDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} active flag is treated as inactive, so toggling turns it on.
     * The slot snapshot is re-warmed with the new state.
     *
     * @param mapperId the experience-time-slot mapping identifier
     * @throws ResourceNotFoundException if no such mapping exists
     */
    @Override
    @Transactional
    public void toggleAttachmentActive(Long mapperId) {
        ExperienceTimeSlotMapper mapper = timeSlotMapperDao.findById(mapperId);
        if (mapper == null)
            throw new ResourceNotFoundException("TimeSlot mapping not found: " + mapperId);
        mapper.setIsActive(!Boolean.TRUE.equals(mapper.getIsActive()));
        ExperienceTimeSlotMapper updated = timeSlotMapperDao.update(mapper);
        catalogCacheService.warmSlotCache(updated);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TimeSlot findTimeSlotOrThrow(Long id) {
        TimeSlot entity = timeSlotDao.findById(id);
        if (entity == null)
            throw new ResourceNotFoundException("TimeSlot not found: " + id);
        return entity;
    }

    private ExperienceLocationMapper resolveExpLocation(Long experienceId, Long locationId) {
        ExperienceLocationMapper expLocation = expLocationMapperDao.findByExperienceIdAndLocationId(experienceId,
                locationId);
        if (expLocation == null) {
            throw new ResourceNotFoundException(
                    "Location " + locationId + " is not attached to experience " + experienceId
                            + ". Attach the location first.");
        }
        return expLocation;
    }
}
