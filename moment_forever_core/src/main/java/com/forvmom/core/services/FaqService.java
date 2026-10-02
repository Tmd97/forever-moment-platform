package com.forvmom.core.services;

import com.forvmom.common.dto.request.FaqRequestDto;
import com.forvmom.common.dto.response.FaqResponseDto;

import java.util.List;

public interface FaqService {

    /** Create a new FAQ. A null displayOrder places it at the end of the list */
    FaqResponseDto createFaq(FaqRequestDto requestDto);

    /** Update an existing FAQ's question/answer/order/active flag */
    FaqResponseDto updateFaq(Long id, FaqRequestDto requestDto);

    /** Soft-delete an FAQ */
    boolean deleteFaq(Long id);

    /** All FAQs for the admin list, including inactive ones */
    List<FaqResponseDto> getAllFaqsAdmin();

    /** Only active FAQs, ordered, for the public FAQ accordion */
    List<FaqResponseDto> getActiveFaqs();

    /** Flip an FAQ's active flag */
    void toggleActive(Long id);

    /**
     * Resequences display order to match the given id order (1-based), as
     * produced by a drag-and-drop reorder in the admin UI.
     *
     * @param orderedIds every FAQ id, in its new display order
     */
    void reorderFaqs(List<Long> orderedIds);
}
