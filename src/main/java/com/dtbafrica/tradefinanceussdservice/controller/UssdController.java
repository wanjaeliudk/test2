package com.dtbafrica.tradefinanceussdservice.controller;

import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdEndRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdResponseRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.request.UssdStartRequest;
import com.dtbafrica.tradefinanceussdservice.domain.dto.response.UssdGatewayResponse;
import com.dtbafrica.tradefinanceussdservice.service.implementation.UssdSessionServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ussd")
@RequiredArgsConstructor
@Validated
public class UssdController {

  private final UssdSessionServiceImpl ussdSessionService;

  @PostMapping("/session/{sessionId}/start")
  public ResponseEntity<UssdGatewayResponse> startSession(
      @PathVariable String sessionId, @Valid @RequestBody UssdStartRequest request) {
    return ResponseEntity.ok(ussdSessionService.startSession(sessionId, request));
  }

  @PutMapping("/session/{sessionId}/response")
  public ResponseEntity<UssdGatewayResponse> handleResponse(
      @PathVariable String sessionId, @Valid @RequestBody UssdResponseRequest request) {
    return ResponseEntity.ok(ussdSessionService.handleResponse(sessionId, request));
  }

  @PutMapping("/session/{sessionId}/end")
  public ResponseEntity<UssdGatewayResponse> endSession(
      @PathVariable String sessionId, @Valid @RequestBody UssdEndRequest request) {
    return ResponseEntity.ok(ussdSessionService.endSession(sessionId, request));
  }
}
