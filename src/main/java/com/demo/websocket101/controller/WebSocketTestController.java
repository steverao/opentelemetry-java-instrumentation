package com.demo.websocket101.controller;

import com.demo.websocket101.service.TestWebSocketClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/test")
public class WebSocketTestController {

  private final TestWebSocketClient webSocketClient;

  public WebSocketTestController(TestWebSocketClient webSocketClient) {
    this.webSocketClient = webSocketClient;
  }

  @GetMapping("/connect")
  public Mono<Map<String, Object>> connect(
      @RequestParam(defaultValue = "client-001") String sessionId) {
    validateSessionId(sessionId);
    return webSocketClient.connect(sessionId).map(message -> response(message));
  }

  @GetMapping("/close")
  public Mono<Map<String, Object>> close(
      @RequestParam(defaultValue = "client-001") String sessionId) {
    validateSessionId(sessionId);
    return webSocketClient.close(sessionId).map(message -> response(message));
  }

  @GetMapping("/status")
  public Map<String, Object> status() {
    return response("current connections");
  }

  private Map<String, Object> response(String message) {
    List<String> activeSessions = webSocketClient.activeSessionIds();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("message", message);
    result.put("activeSessionCount", activeSessions.size());
    result.put("activeSessionIds", activeSessions);
    return result;
  }

  private static void validateSessionId(String sessionId) {
    if (!sessionId.matches("[A-Za-z0-9._-]+")) {
      throw new IllegalArgumentException(
          "sessionId may contain only letters, digits, dot, underscore, and hyphen");
    }
  }
}
