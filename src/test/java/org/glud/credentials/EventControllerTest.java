package org.glud.credentials;

import org.glud.credentials.event.controller.EventController;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;
import org.glud.credentials.event.dto.EventResponseDTO;
import org.glud.credentials.event.dto.EventStatus;
import org.glud.credentials.event.service.EventService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class EventControllerTest {

    private static final EventResponseDTO EVENT_DTO = new EventResponseDTO(
            50L, "Asamblea GLUD", LocalDateTime.of(2026, 8, 10, 18, 0),
            EventStatus.SCHEDULED, 0L, 1L, "GLUD", "99999999999");

    private static final EventAttendanceResponseDTO ATTENDEE_DTO = new EventAttendanceResponseDTO(
            1L, 2L, "20210000002", "María Gómez",
            LocalDateTime.of(2026, 8, 10, 18, 5), "20219999999");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    void findAll_returns200WithEvents() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(EVENT_DTO));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value(50))
                .andExpect(jsonPath("$[0].title").value("Asamblea GLUD"));
    }

    @Test
    void create_returns201WithEvent() throws Exception {
        when(eventService.create(any())).thenReturn(EVENT_DTO);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Asamblea GLUD", "startsAt": "2026-08-10T18:00:00" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(50))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void create_returns400_whenTitleBlank() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "   ", "startsAt": "2026-08-10T18:00:00" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returns200WithEvent() throws Exception {
        when(eventService.findById(50L)).thenReturn(EVENT_DTO);

        mockMvc.perform(get("/api/events/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(50))
                .andExpect(jsonPath("$.tenantName").value("GLUD"));
    }

    @Test
    void attendees_returns200WithList() throws Exception {
        when(eventService.attendees(50L)).thenReturn(List.of(ATTENDEE_DTO));

        mockMvc.perform(get("/api/events/50/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("20210000002"));
    }

    @Test
    void exportAttendance_returnsCsvWithAttachmentHeader() throws Exception {
        when(eventService.exportAttendeesCsv(50L)).thenReturn(
                "\uFEFFEvento;\"Asamblea GLUD\"\r\nAsistentes;1\r\n");

        mockMvc.perform(get("/api/events/50/attendance/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"asistentes-evento-50.csv\""))
                .andExpect(result -> result.getResponse().getContentType().startsWith("text/csv"))
                .andExpect(result -> result.getResponse().getContentAsString()
                        .contains("Asamblea GLUD"));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(eventService).delete(eq(50L));

        mockMvc.perform(delete("/api/events/50"))
                .andExpect(status().isNoContent());
    }
}
