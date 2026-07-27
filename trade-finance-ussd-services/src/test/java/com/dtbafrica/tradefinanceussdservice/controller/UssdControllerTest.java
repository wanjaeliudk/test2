package com.dtbafrica.tradefinanceussdservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdResponseRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdStartRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.response.UssdGatewayResponse;
import com.dtbafrica.tradefinanceussdservice.service.UssdSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UssdController.class,
    excludeAutoConfiguration = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      JpaRepositoriesAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
class UssdControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UssdSessionService ussdSessionService;

  @Test
  void startSessionReturnsRenderedMenu() throws Exception {
    when(ussdSessionService.startSession(eq("session-1"), any(UssdStartRequest.class)))
        .thenReturn(new UssdGatewayResponse(false, "Welcome to DTB", 200, ""));

    mockMvc
        .perform(
            post("/ussd/session/session-1/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"msisdn\":\"254700000001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ussdMenu").value("Welcome to DTB"))
        .andExpect(jsonPath("$.responseExitCode").value(200));
  }

  @Test
  void responseEndpointReturnsRenderedMenu() throws Exception {
    when(ussdSessionService.handleResponse(eq("session-1"), any(UssdResponseRequest.class)))
        .thenReturn(new UssdGatewayResponse(false, "Select anchor", 200, ""));

    mockMvc
        .perform(
            put("/ussd/session/session-1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"msisdn\":\"254700000001\",\"text\":\"1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ussdMenu").value("Select anchor"))
        .andExpect(jsonPath("$.responseExitCode").value(200));
  }

  @Test
  void endEndpointReturnsCloseResponse() throws Exception {
    when(ussdSessionService.endSession(eq("session-1"), any()))
        .thenReturn(new UssdGatewayResponse(true, "Thank you for using DTB USSD.", 200, ""));

    mockMvc
        .perform(
            put("/ussd/session/session-1/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"user_exit\",\"exitCode\":200}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shouldClose").value(true))
        .andExpect(jsonPath("$.responseExitCode").value(200));
  }
}
