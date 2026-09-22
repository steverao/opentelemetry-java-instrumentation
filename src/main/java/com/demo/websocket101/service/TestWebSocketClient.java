package com.demo.websocket101.service;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class TestWebSocketClient {

  private static final Logger logger = LoggerFactory.getLogger(TestWebSocketClient.class);

  private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final Map<String, Disposable> connections = new ConcurrentHashMap<>();
  private final int serverPort;

  public TestWebSocketClient(@Value("${server.port:8093}") int serverPort) {
    this.serverPort = serverPort;
  }

  public Mono<String> connect(String sessionId) {
    if (sessions.containsKey(sessionId)) {
      return Mono.just("already connected: " + sessionId);
    }

    URI uri = URI.create("ws://127.0.0.1:" + serverPort + "/dds_gwm/" + sessionId);
    Sinks.One<String> connected = Sinks.one();

    Disposable connection =
        client
            .execute(
                uri,
                session -> {
                  sessions.put(sessionId, session);
                  connected.tryEmitValue("connected: " + uri);
                  logger.info("Test client connected: sessionId={}, webSocketId={}", sessionId, session.getId());

                  Mono<Void> hello =
                      session.send(
                          Mono.just(session.textMessage("hello-from-test-client-" + sessionId)));

                  Mono<Void> receive =
                      session
                          .receive()
                          .doOnNext(
                              message ->
                                  logger.debug(
                                      "Test client received: sessionId={}, message={}",
                                      sessionId,
                                      message.getPayloadAsText()))
                          .then();

                  return hello
                      .then(receive)
                      .doFinally(
                          signal -> {
                            sessions.remove(sessionId, session);
                            logger.info(
                                "Test client disconnected: sessionId={}, signal={}",
                                sessionId,
                                signal);
                          });
                })
            .doOnError(connected::tryEmitError)
            .doFinally(signal -> connections.remove(sessionId))
            .subscribe();

    Disposable previous = connections.putIfAbsent(sessionId, connection);
    if (previous != null) {
      connection.dispose();
      return Mono.just("connection is already being established: " + sessionId);
    }

    return connected
        .asMono()
        .timeout(Duration.ofSeconds(5))
        .doOnError(
            error -> {
              Disposable disposable = connections.remove(sessionId);
              if (disposable != null) {
                disposable.dispose();
              }
            });
  }

  public Mono<String> close(String sessionId) {
    WebSocketSession session = sessions.get(sessionId);
    if (session == null) {
      return Mono.just("not connected: " + sessionId);
    }
    return session.close(CloseStatus.NORMAL).thenReturn("close requested: " + sessionId);
  }

  public List<String> activeSessionIds() {
    List<String> result = new ArrayList<>(sessions.keySet());
    Collections.sort(result);
    return result;
  }
}
