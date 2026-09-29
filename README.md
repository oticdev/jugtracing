# jugtracing

Demo project for the Naija JUG talk on **distributed tracing with OpenTelemetry and Grafana**.

A small order flow built on Spring Boot 4.1 and Spring Cloud 2025.1: an API gateway, a service registry and three services. They call each other over HTTP and over Kafka. Each request becomes **one trace** across every service, stored in Tempo, linked to its logs in Loki, and viewed in Grafana.

## Architecture

```
                      ┌──────────────────┐
                      │ discovery-server │  Eureka :8761
                      └──────────────────┘
                         ▲ register / lookup
 client ──► api-gateway ─┼──► order-service ──HTTP──► inventory-service ──► Postgres (inventory_db)
            :8080        │      :8081      (GET stock,    :8082
                         │        │         POST reserve)
                         │        ├──► Postgres (orders_db)
                         │        └──► Kafka topic "order-events" ──► notification-service :8083

 gateway, order, inventory, notification ──OTLP/HTTP :4318──► OTel Collector ──► Tempo (traces)
                                                                             └──► Loki  (logs)
                                                               Grafana :3000 reads Tempo + Loki
```

| Component            | Port | What it does |
|----------------------|------|--------------|
| api-gateway          | 8080 | Spring Cloud Gateway (WebMVC). Routes `/api/orders/**` and `/api/inventory/**` via `lb://` |
| discovery-server     | 8761 | Eureka registry, with a dashboard at http://localhost:8761. Deliberately not traced |
| order-service        | 8081 | Places orders. Calls inventory, saves the order and publishes `OrderPlacedEvent` |
| inventory-service    | 8082 | Looks up and reserves stock. Has chaos knobs for latency and failures |
| notification-service | 8083 | Consumes `order-events` and "sends" an email (a log line and a 200 ms sleep) |
| Postgres             | 5433 | `orders_db` and `inventory_db`. Tables are created by Hibernate (`ddl-auto=update`) |
| Kafka                | 9092 | Single-node KRaft broker |
| Kafka UI             | 8090 | http://localhost:8090, for browsing `order-events` messages and their `traceparent`/`baggage` headers |
| OTel Collector       | 4317 / 4318 | Receives OTLP (gRPC/HTTP) from the services. Traces go to Tempo, logs to Loki |
| Tempo                | 3200 | Trace storage. Also answers TraceQL metrics queries (rate, errors, p95), so no Prometheus is needed |
| Loki                 | 3100 | Log storage, with `trace_id`, `span_id` and `customer_tier` on every line |
| Grafana              | 3000 | http://localhost:3000, no login needed. Opens on the **Naija JUG – Tracing Overview** dashboard |

## Prerequisites

- **JDK 25.** Make sure `JAVA_HOME` points at it for `./mvnw`, e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`.
- **Docker Desktop.**
- **IntelliJ IDEA.** When you open the project, load it as a Maven project.

## Run it

### Startup order

Start things in this order. Wait for each step to be ready before you start the next one.

| # | What | How to start it | Ready when |
|---|------|-----------------|------------|
| 1 | Infrastructure (Postgres, Kafka, Kafka UI, Collector, Tempo, Loki, Grafana) | `docker compose up -d` | `docker compose ps` shows postgres and kafka as `healthy`, and http://localhost:3000 loads |
| 2 | `discovery-server` | Run `DiscoveryServerApplication` | The dashboard at http://localhost:8761 loads |
| 3 | `inventory-service` | Run `InventoryServiceApplication` | The log shows `Seeded 5 products` (first run only) or `Started` |
| 4 | `order-service` | Run `OrderServiceApplication` | The log shows `Started` |
| 5 | `notification-service` | Run `NotificationServiceApplication` | The log shows partitions assigned for `order-events` |
| 6 | `api-gateway` | Run `ApiGatewayApplication` | The log shows `Started` |
| 7 | Wait about 30 s | – | http://localhost:8761 lists all four services as UP |
| 8 | Optional: background traffic | `docker compose --profile load up -d k6` | The dashboard panels fill up |

Why this order:
- **Infrastructure first.** order-service and inventory-service fail on startup without Postgres. order-service and notification-service need Kafka. If the Collector isn't up yet, the services still run, but the first traces and logs are lost.
- **Discovery second.** Every other service registers with it.
- **Inventory before order.** order-service calls inventory for every request.
- **Gateway last.** It's the front door, and it can only route to services that are already registered.

Steps 3 to 6 aren't strict. Services retry Eureka registration by themselves. The ~30 s wait in step 7 is the Eureka registration and client cache refresh. Until it has passed, the gateway or order-service can return 503 "no instances available".

### Starting the services

Use **one** of these two options.

- **IntelliJ:** run the shared **All services** configuration from `.run/`. It starts the services in the order above. You can also run the individual configurations one by one.
- **Terminal:** build once with `./mvnw clean package`, then start each service in its own terminal:

```bash
java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar
java -jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

**Shutdown** runs in reverse:
1. Stop k6 with `docker compose --profile load stop k6`, if it's running.
2. Stop the services.
3. Run `docker compose down`. Add `-v` to wipe the data, including traces.

Once everything shows as UP on http://localhost:8761, you can:
- run the requests in [`http/demo.http`](http/demo.http) (IntelliJ HTTP client), or
- use curl:

```bash
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -H 'baggage: customer.tier=gold' \
  -d '{"sku":"JOLLOF-001","quantity":2,"customerEmail":"ada@naijajug.ng"}'
```

Seeded SKUs:

| SKU        | Stock   | Note |
|------------|---------|------|
| JOLLOF-001 | 100 000 |      |
| SUYA-001   | 100 000 |      |
| PUFF-001   | 100 000 |      |
| CHIN-001   | 100 000 |      |
| ZOBO-001   | 5       | Use this one for the out-of-stock demo |

Stock is seeded only into an empty table. To reset all data, run `docker compose down -v`.

## Chaos knobs (inventory-service)

```bash
# 1.5 s extra latency on every inventory call (an order makes two calls, so about 3 s end to end)
curl -X POST localhost:8080/api/inventory/chaos -H 'Content-Type: application/json' -d '{"latencyMs":1500,"failureRate":0}'
# Fail half the calls with 503. order-service then answers 502
curl -X POST localhost:8080/api/inventory/chaos -H 'Content-Type: application/json' -d '{"latencyMs":0,"failureRate":0.5}'
# Back to normal
curl -X POST localhost:8080/api/inventory/chaos -H 'Content-Type: application/json' -d '{"latencyMs":0,"failureRate":0}'
```

## Demo script (for the talk)

1. **One request, one trace.**
   - Send request 2 from `demo.http` (a gold jollof order).
   - Copy the trace ID from the order-service console. Every line looks like `[<traceId>,<spanId>,tier=gold]`.
   - In Grafana, go to **Explore → Tempo** and paste the trace ID. The waterfall shows 11 spans:
     1. `api-gateway`: the server span, then the proxy client span
     2. `order-service`: the server span, then `place-order`, a custom span with `order.*` attributes and `stock.checked`/`stock.reserved` events
     3. Two inventory calls, each as a client → server span pair. The reserve span carries `inventory.remaining`.
     4. `order-events send` (Kafka producer)
     5. `order-events process` in notification-service (Kafka consumer)
     6. `send-email`

     Switch to the **Node graph** view to see the same trace as a service map.
2. **Traces ↔ logs.**
   - Open a span and click **Logs for this span**. Loki shows only that trace's log lines, across all services.
   - In Loki, click **View trace** on any line to jump back to the trace.
3. **Baggage.**
   - The `baggage: customer.tier=gold` header was set once by the client.
   - It travels through the gateway, the HTTP calls and the Kafka headers.
   - It appears on the spans each service creates and on every log line (`customer_tier` in Loki).
4. **Dashboard.**
   - Start k6 with `docker compose --profile load up -d k6`.
   - The **Naija JUG – Tracing Overview** dashboard shows request rate, error rate and p95 latency per service. All of it comes from **TraceQL metrics**, computed by Tempo from the spans, with no Prometheus.
   - Click a point on a chart (an exemplar) to open an example trace.
   - The **Traces Drilldown** app (Grafana menu → Drilldown → Traces) does the same with no query at all.
5. **Chaos.**
   - Set `failureRate` to 0.3 and `latencyMs` to 1100.
   - The error and latency panels rise, and the *Slow or chaos traces* table fills.
   - Open a failed trace. The red spans lead straight to the root cause: inventory's 503, which order-service turns into a 502.
   - **Bonus finding:** the inventory GET appears **twice** under a single client span. Spring Cloud LoadBalancer quietly retried it, and only the trace reveals it.
6. **Tail sampling.**
   - Switch the Collector to its tail-sampling config:

     ```bash
     COLLECTOR_CONFIG=otel-collector-tail-sampling.yaml docker compose up -d otel-collector
     ```

   - The Collector now keeps all errors, all traces over 1 s, all `gold` customers, and 20% of everything else. The request-rate panel drops accordingly.
   - Talking point: the dashboard is now computed from *sampled* data, and errors look over-represented. That's why production setups compute RED metrics in the Collector *before* sampling, usually into Prometheus.
   - Switch back with `docker compose up -d otel-collector`.

## How the tracing is wired

**Shared module `observability-support`.** Every traced service depends on it. It brings:
- `spring-boot-starter-opentelemetry`: Micrometer Observation → OpenTelemetry bridge → OTLP.
- The OpenTelemetry Logback appender. Its version must match the OTel API that Boot manages; see `pom.xml`.
- Three small auto-configurations:
  - `OpenTelemetryLoggingAutoConfiguration` hands Boot's OpenTelemetry SDK to the Logback appender, so logs are exported.
  - `ObservationNoiseFilterAutoConfiguration` drops Eureka heartbeat and `/actuator` observations. Otherwise every service creates an orphan trace every 30 s.
  - `BaggageSpanTaggingAutoConfiguration` copies incoming baggage (`customer.tier`) onto spans. Boot's `tag-fields` alone only covers baggage created in code.

**Per-service config.** Each service's `application.yml` has:
- `management.opentelemetry.*`: the OTLP endpoints and resource attributes.
- `management.tracing.baggage.*`: which baggage fields to propagate, put in logs, and tag on spans.
- `management.tracing.sampling.probability: 1.0`: export everything and let the Collector decide.
- `spring.kafka.template/listener.observation-enabled`: Kafka producer and consumer spans. They also carry the trace across Kafka in record headers.
- Metrics export is switched off, because there's no metrics backend.

**Code-level tracing.**
- **HTTP:** `InventoryClient` is an `@HttpExchange` interface registered with `@ImportHttpServices`.
  - Boot builds its `RestClient` from the auto-configured builder, which carries the `ObservationRegistry`.
  - Spring Cloud adds Eureka load balancing (`lb://inventory-service`).
  - Avoid `RestClient.create()`: it skips observation and breaks propagation.
- **Custom spans:** `OrderService` (`place-order`) and `EmailSender` (`send-email`) use the Observation API directly. `ChaosMonkey` and `InventoryService` tag the current span through the `Tracer`.
- **Handled errors:** a `@RestControllerAdvice` that turns an exception into a 5xx would leave the server span looking healthy. Both `ApiExceptionHandler`s call `observationRegistry.getCurrentObservation().error(e)`, so the span is red, counted by the error panel, and kept by tail sampling.
