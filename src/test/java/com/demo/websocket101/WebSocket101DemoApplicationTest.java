package com.demo.websocket101;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.port=0")
class WebSocket101DemoApplicationTest {

  @Test
  void contextLoads() {}
}
