package com.forvmom.core.controller.admin;

import com.forvmom.common.errorhandler.GlobalExceptionHandler;
import com.forvmom.common.errorhandler.IdempotencyConflictException;
import com.forvmom.common.errorhandler.IdempotencyRequestInProgressException;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import com.forvmom.core.services.BookingOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerAdminTest {

    private static final String REQUEST_BODY = """
            {
              "timeSlotMapperId": 100,
              "bookingDate": "2099-08-24",
              "guestCount": 2,
              "pincode": "560001",
              "addonMapperIds": [3]
            }
            """;

    private BookingOrchestrationService orchestrationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orchestrationService = mock(BookingOrchestrationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BookingControllerAdmin(orchestrationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void requiresStandardIdempotencyKeyHeader() throws Exception {
        mockMvc.perform(post("/admin/bookings")
                        .header("X-User-Id", "7")
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Idempotency-Key")));
    }

    @Test
    void firstRequestReturnsAcceptedResponseWithoutReplayHeader() throws Exception {
        String body = "{\"code\":200,\"status\":\"SUCCESS\",\"bookingId\":\"MFB-100\"}";
        when(orchestrationService.initiateBooking(any(), eq(7L), eq("key-1")))
                .thenReturn(new BookingInitiationResult("MFB-100", 202, body, false));

        mockMvc.perform(post("/admin/bookings")
                        .header("X-User-Id", "7")
                        .header("X-User-Roles", "USER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(content().json(body));
    }

    @Test
    void replayReturnsStoredStatusAndBodyWithReplayHeader() throws Exception {
        String body = "{\"code\":200,\"status\":\"SUCCESS\",\"bookingId\":\"MFB-100\"}";
        when(orchestrationService.initiateBooking(any(), eq(7L), eq("key-1")))
                .thenReturn(new BookingInitiationResult("MFB-100", 202, body, true));

        mockMvc.perform(post("/admin/bookings")
                        .header("X-User-Id", "7")
                        .header("X-User-Roles", "USER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(content().json(body));
    }

    @Test
    void keyReuseForDifferentRequestReturnsStructuredConflict() throws Exception {
        when(orchestrationService.initiateBooking(any(), eq(7L), eq("key-1")))
                .thenThrow(new IdempotencyConflictException("different request"));

        mockMvc.perform(post("/admin/bookings")
                        .header("X-User-Id", "7")
                        .header("X-User-Roles", "USER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().json(
                        "{\"code\":409,\"status\":\"CONFLICT\",\"msg\":\"different request\"}"));
    }

    @Test
    void activeOwnerReturnsConflictWithRetryAfter() throws Exception {
        when(orchestrationService.initiateBooking(any(), eq(7L), eq("key-1")))
                .thenThrow(new IdempotencyRequestInProgressException("still in progress"));

        mockMvc.perform(post("/admin/bookings")
                        .header("X-User-Id", "7")
                        .header("X-User-Roles", "USER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(content().json(
                        "{\"code\":409,\"status\":\"CONFLICT\",\"msg\":\"still in progress\"}"));
    }
}
