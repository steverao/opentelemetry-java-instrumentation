# Netty WebSocket HTTP 101 server span reproducer

This small Spring WebFlux application reproduces a Netty 4.1 server span that is started for a
WebSocket HTTP upgrade request but is never ended or exported.

The application contains:

- a WebSocket server route registered as `/dds_gwm/*`;
- a `DDSWebSocketHandler` whose controller span makes the missing parent easy to identify;
- a built-in Reactor Netty WebSocket client;
- HTTP endpoints that establish and normally close the WebSocket connection.

The built-in client is used so that the reproducer sends a normal WebSocket close frame instead of
relying on an abruptly terminated client process.

The reproduction does not manually supply a `traceparent` header. The bug is reproducible when the
agent creates the trace for the incoming WebSocket upgrade request normally, without a fixed trace
ID or an externally forced sampling decision.

## Tested environment

- OpenTelemetry Java agent 2.31.1
- Amazon Corretto 21.0.7
- Spring Boot 2.7.18
- Spring WebFlux / Reactor Netty
- macOS 15.1.1

## Build

```shell
mvn clean package
```

Download the agent:

```shell
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.31.1/opentelemetry-javaagent.jar
```

## Run

Use the logging exporter and enable controller telemetry so that the child
`DDSWebSocketHandler.handle` span is visible:

```shell
java \
  -javaagent:./opentelemetry-javaagent.jar \
  -Dotel.service.name=websocket-101-reproducer \
  -Dotel.traces.exporter=logging \
  -Dotel.metrics.exporter=none \
  -Dotel.logs.exporter=none \
  -Dotel.instrumentation.common.experimental.controller-telemetry.enabled=true \
  -jar target/websocket-101-demo-1.0.0.jar
```

Open the WebSocket connection:

```shell
curl --fail 'http://127.0.0.1:8093/test/connect?sessionId=normal-close'
```

No `traceparent` header or other tracing header needs to be added to this request.

The application reports one active session and the logging exporter emits
`DDSWebSocketHandler.handle`. Keep the application running, then close the same session normally:

```shell
curl --fail 'http://127.0.0.1:8093/test/close?sessionId=normal-close'
```

The server and client logs both report `signal=onComplete`, confirming a normal WebSocket close.

## Expected result

The Netty HTTP server span for the upgrade request should be ended and exported either when the
`101 Switching Protocols` response is written or, under the current long-lived-span design, when
the WebSocket channel is closed:

```text
GET /dds_gwm/*                  SERVER
└── DDSWebSocketHandler.handle INTERNAL
```

## Actual result

The child span is exported:

```text
'DDSWebSocketHandler.handle' ... INTERNAL
  [tracer: io.opentelemetry.spring-webflux-5.0:2.31.1-alpha]
```

The `io.opentelemetry.netty-4.1` server span for `/dds_gwm/*` is not exported while the connection
is active, after the normal close completes, after an additional 30-second wait, or during graceful
application shutdown.

Ordinary HTTP requests in the same application, such as `GET /test/connect` and
`GET /test/close`, produce Netty server spans normally. This confirms that the logging exporter and
Netty server instrumentation are active.

## Relevant implementation

For a `FullHttpResponse` with status 101,
`HttpServerResponseTracingHandler` records `ProtocolSpecificEvent.SWITCHING_PROTOCOLS` but does not
remove the corresponding `ServerContext` or call `instrumenter.end(...)`. The remaining server
context is expected to be ended by `HttpServerRequestTracingHandler.channelInactive()`. In this
Spring WebFlux WebSocket pipeline, the close path does not end/export that server span.
