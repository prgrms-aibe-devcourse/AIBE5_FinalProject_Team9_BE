package com.grimgate.grimgate_backend.domain.theme.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotReleaseRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotReleaseResponse;
import com.grimgate.grimgate_backend.domain.theme.service.SlotHoldService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(SlotHoldController.class)
@AutoConfigureMockMvc(addFilters = false)
class SlotHoldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SlotHoldService slotHoldService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /api/slots/{id}/hold - 슬롯 HOLD 성공")
    void holdSlot_Success() throws Exception {
        Long timeSlotId = 1L;
        SlotHoldResponse response = SlotHoldResponse.builder()
                .timeSlotId(timeSlotId)
                .holdToken("hold-token-xyz")
                .expiresInSeconds(300L)
                .build();

        when(slotHoldService.holdSlot(eq(timeSlotId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/slots/{id}/hold", timeSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlotId").value(timeSlotId))
                .andExpect(jsonPath("$.holdToken").value("hold-token-xyz"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    @DisplayName("PATCH /api/slots/{id}/release - 슬롯 HOLD 해제 성공")
    void releaseSlot_Success() throws Exception {
        Long timeSlotId = 1L;
        SlotReleaseRequest request = SlotReleaseRequest.builder()
                .holdToken("hold-token-xyz")
                .build();
        SlotReleaseResponse response = SlotReleaseResponse.builder()
                .timeSlotId(timeSlotId)
                .released(true)
                .build();

        when(slotHoldService.releaseSlot(eq(timeSlotId), any(SlotReleaseRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/slots/{id}/release", timeSlotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlotId").value(timeSlotId))
                .andExpect(jsonPath("$.released").value(true));
    }

    @Test
    @DisplayName("PATCH /api/slots/{id}/release - 선점 정보가 존재하지 않아 404 에러 반환")
    void releaseSlot_NotFound() throws Exception {
        Long timeSlotId = 1L;
        SlotReleaseRequest request = SlotReleaseRequest.builder()
                .holdToken("hold-token-xyz")
                .build();

        when(slotHoldService.releaseSlot(eq(timeSlotId), any(SlotReleaseRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "선점 정보가 존재하지 않습니다."));

        mockMvc.perform(patch("/api/slots/{id}/release", timeSlotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/slots/{id}/release - 선점 정보가 일치하지 않아 409 에러 반환")
    void releaseSlot_Conflict() throws Exception {
        Long timeSlotId = 1L;
        SlotReleaseRequest request = SlotReleaseRequest.builder()
                .holdToken("wrong-token")
                .build();

        when(slotHoldService.releaseSlot(eq(timeSlotId), any(SlotReleaseRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "선점 정보가 일치하지 않습니다."));

        mockMvc.perform(patch("/api/slots/{id}/release", timeSlotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
