package com.demo.websocket101.handler;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DDSWebSocketHandler implements WebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(DDSWebSocketHandler.class);
  private final AtomicLong heartbeatSequence = new AtomicLong();

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    logger.info("WebSocket connected: id={}, uri={}", session.getId(), session.getHandshakeInfo().getUri());

    Mono<Void> inbound =
        session
            .receive()
            .doOnNext(message -> logger.info("Received from {}: {}", session.getId(), message.getPayloadAsText()))
            .then();

    Flux<WebSocketMessage> heartbeats =
        Flux.interval(Duration.ofSeconds(1))
            .takeUntilOther(session.closeStatus())
            .map(
                ignored ->
                    session.textMessage(
                        "heartbeat-" + heartbeatSequence.incrementAndGet()));

    return Mono.when(inbound, session.send(heartbeats))
        .doFinally(signal -> logger.info("WebSocket disconnected: id={}, signal={}", session.getId(), signal));
  }
}
