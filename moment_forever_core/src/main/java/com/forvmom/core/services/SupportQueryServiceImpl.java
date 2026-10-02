package com.forvmom.core.services;

import com.forvmom.common.dto.request.SupportQueryRequestDto;
import com.forvmom.common.dto.response.SupportQueryResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.mapper.SupportQueryBeanMapper;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.SupportQueryDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.SupportQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JPA-backed implementation of {@link SupportQueryService}.
 *
 * <p>
 * This is a simple one-shot contact form, not a reply-thread ticketing system:
 * a query is either {@code OPEN} or {@code RESOLVED}.
 */
@Service
public class SupportQueryServiceImpl implements SupportQueryService {

    @Autowired
    private SupportQueryDao supportQueryDao;

    @Autowired
    private ApplicationUserDao applicationUserDao;

    @Override
    @Transactional
    public SupportQueryResponseDto submitQuery(SupportQueryRequestDto requestDto, Long authUserId) {
        SupportQuery entity = new SupportQuery();

        if (authUserId != null) {
            ApplicationUser user = applicationUserDao.findByAuthUserId(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authUserId));
            entity.setApplicationUser(user);
            entity.setName(user.getFullName());
            entity.setEmail(user.getEmail());
            entity.setPhone(user.getPhoneNumber());
        } else {
            if (isBlank(requestDto.getName()) || isBlank(requestDto.getEmail()) || isBlank(requestDto.getPhone())) {
                throw new IllegalArgumentException(
                        "name, email and phone are required for a guest support query");
            }
            entity.setName(requestDto.getName());
            entity.setEmail(requestDto.getEmail());
            entity.setPhone(requestDto.getPhone());
        }

        entity.setSubject(requestDto.getSubject());
        entity.setMessage(requestDto.getMessage());
        entity.setStatus(SupportQuery.STATUS_OPEN);
        entity.setReferenceId(generateReferenceId());

        SupportQuery saved = supportQueryDao.save(entity);
        return SupportQueryBeanMapper.mapEntityToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportQueryResponseDto> getAllQueriesAdmin(String status) {
        return SupportQueryBeanMapper.mapEntitiesToDtos(supportQueryDao.findAll(status));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportQueryResponseDto getQueryById(Long id) {
        SupportQuery existing = supportQueryDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Support query not found: " + id);
        return SupportQueryBeanMapper.mapEntityToDto(existing);
    }

    @Override
    @Transactional
    public SupportQueryResponseDto markResolved(Long id) {
        SupportQuery existing = supportQueryDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Support query not found: " + id);
        existing.setStatus(SupportQuery.STATUS_RESOLVED);
        existing.setResolvedOn(new Date());
        return SupportQueryBeanMapper.mapEntityToDto(supportQueryDao.update(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportQueryResponseDto> getMyQueries(Long authUserId) {
        ApplicationUser user = applicationUserDao.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authUserId));
        return SupportQueryBeanMapper.mapEntitiesToDtos(supportQueryDao.findByApplicationUserId(user.getId()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String generateReferenceId() {
        return AppConstants.SUPPORT_REFERENCE_PREFIX
                + System.currentTimeMillis()
                + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
