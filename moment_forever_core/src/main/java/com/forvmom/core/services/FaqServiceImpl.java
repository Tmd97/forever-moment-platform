package com.forvmom.core.services;

import com.forvmom.common.dto.request.FaqRequestDto;
import com.forvmom.common.dto.response.FaqResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.FaqBeanMapper;
import com.forvmom.data.dao.FaqDao;
import com.forvmom.data.entities.Faq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed implementation of {@link FaqService}.
 *
 * <p>
 * FAQs are a flat, globally-ordered list with no categories or per-experience
 * attachment. Ordering is managed explicitly by the admin via
 * {@link #reorderFaqs(List)} rather than the generic reordering mechanism used
 * elsewhere in the codebase.
 */
@Service
public class FaqServiceImpl implements FaqService {

    @Autowired
    private FaqDao faqDao;

    @Override
    @Transactional
    public FaqResponseDto createFaq(FaqRequestDto requestDto) {
        Faq entity = FaqBeanMapper.mapRequestToEntity(requestDto);
        if (requestDto.getDisplayOrder() == null) {
            entity.setDisplayOrder(faqDao.getMaxDisplayOrder() + 1);
        }
        Faq saved = faqDao.save(entity);
        return FaqBeanMapper.mapEntityToDto(saved);
    }

    @Override
    @Transactional
    public FaqResponseDto updateFaq(Long id, FaqRequestDto requestDto) {
        Faq existing = faqDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("FAQ not found: " + id);
        FaqBeanMapper.updateEntityFromRequest(existing, requestDto);
        return FaqBeanMapper.mapEntityToDto(faqDao.update(existing));
    }

    @Override
    @Transactional
    public boolean deleteFaq(Long id) {
        Faq existing = faqDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("FAQ not found: " + id);
        faqDao.delete(existing);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponseDto> getAllFaqsAdmin() {
        return FaqBeanMapper.mapEntitiesToDtos(faqDao.findAllOrdered());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponseDto> getActiveFaqs() {
        return FaqBeanMapper.mapEntitiesToDtos(faqDao.findAllActiveOrdered());
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Faq existing = faqDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("FAQ not found: " + id);
        existing.setIsActive(!Boolean.TRUE.equals(existing.getIsActive()));
        faqDao.update(existing);
    }

    @Override
    @Transactional
    public void reorderFaqs(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty())
            return;
        int order = 1;
        for (Long id : orderedIds) {
            Faq existing = faqDao.findById(id);
            if (existing == null)
                throw new ResourceNotFoundException("FAQ not found: " + id);
            existing.setDisplayOrder(order++);
            faqDao.update(existing);
        }
    }
}
