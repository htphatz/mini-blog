# Phase 9: Distributed Observability & Monitoring (Zipkin, Prometheus, Grafana)

## 1. Architectural Motivation (Why Distributed Observability?)

In a monolithic application, troubleshooting a bug or performance degradation is straightforward: inspect stack traces in a single log file (`application.log`) or attach a debugger to the local JVM process.

In a **Microservices Architecture**, a single user action (e.g., publishing a blog post) triggers a cascade of asynchronous and synchronous network interactions:
`Client` -> `API Gateway` -> `post-service` -> `profile-service` -> `identity-service` -> `Kafka` -> `notification-service`.

### The "Needle in a Haystack" Problem:
1. **Isolated Logs**: Each service writes logs to its own isolated container stdout. Correlating logs across 6 different services for a specific user failure is nearly impossible without a unified identifier.
2. **Invisible Bottlenecks**: If a request takes 5 seconds to complete, which service or database query caused the delay?
3. **Cascading Silent Failures**: Unmonitored background workers or Kafka consumers failing silently without alerting operators.

---

## 2. The 3 Pillars of Observability

```
                      +-----------------------------------+
                      |   OBSERVABILITY ECOSYSTEM        |
                      +-----------------------------------+
                       /                |                \
                      /                 |                 \
                     v                  v                  v
          +-------------------+ +-------------------+ +-------------------+
          |     METRICS       | |      LOGS         | |     TRACES        |
          | (Numeric Values)  | | (Text Records)    | | (Request Paths)   |
          | Prometheus/Grafana| | Loki / ELK        | | Micrometer/Zipkin |
          +-------------------+ +-------------------+ +-------------------+
```

1. **Metrics**: Aggregated numerical measurements recorded over time intervals (e.g., CPU %, Memory usage, HTTP request rate RPS, error rates, P95/P99 latency). Used for real-time alerting and dashboard visualization.
2. **Logs**: Discrete, timestamped event records emitted by code (`log.info()`, `log.error()`). Used for deep root-cause debugging.
3. **Traces**: Visual representation of the complete execution journey of a request across all service boundaries and network hops.

---

## 3. Under-the-Hood Mechanics of Distributed Tracing

### A. Trace ID, Span ID, and Call Trees

```
[API Gateway]  ====================== Trace ID: 4bf92f35... ======================>
  | (Span A: Gateway Route, 150ms)
  +---> [post-service]
          | (Span B: Fetch Post, 120ms)
          +---> [profile-service]
                  | (Span C: Neo4j Query, 80ms)
```

- **Trace ID**: A globally unique 128-bit identifier assigned at the API Gateway when a request enters the system. The Trace ID remains unchanged as the request travels across all downstream services, databases, and message brokers.
- **Span ID**: A unique 64-bit identifier assigned to each individual operation or network segment (e.g., Gateway Filter, Controller method, OpenFeign call, gRPC invocation, DB query).
- **Parent Span ID**: Establishes parent-child relationships, allowing tracing UI tools (Zipkin, Jaeger) to render a visual Directed Acyclic Graph (DAG) timeline tree.

### B. Context Propagation Across Network Boundaries

To pass tracing metadata across HTTP requests or Kafka messages, tracing libraries inject standardized HTTP headers:

- **W3C Trace Context Standard** (Modern Standard):
  `traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`
  - `00`: Version
  - `4bf92f35...`: 128-bit Trace ID
  - `00f067aa...`: 64-bit Parent Span ID
  - `01`: Trace Flags (Sampled = true)
- **B3 Propagation Specification** (Zipkin legacy):
  `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled`

When `post-service` calls `profile-service` via OpenFeign or gRPC, the tracing interceptor automatically serializes the `traceparent` header into the outgoing request. The receiving service extracts the header, continues the same Trace ID, and creates a child Span ID.

---

## 4. Metrics Collection: Prometheus Pull Model

Unlike traditional agents that push metrics to a central server (causing network overhead), **Prometheus** uses an active **Pull Model (Scraping)**:

```
+-------------------+                          +-------------------+
|  Prometheus Server| --- Scrapes every 15s -> |  post-service     |
|   (Port 9090)     |                          | /actuator/prometheus
+-------------------+                          +-------------------+
          |
          | Queries via PromQL
          v
+-------------------+
| Grafana Dashboard | (Visualizes CPU, Latency, Error Rate, Thread Counts)
|   (Port 3000)     |
+-------------------+
```

1. Each Spring Boot microservice exposes a `/actuator/prometheus` endpoint powered by **Micrometer**.
2. Prometheus periodically scrapes metrics over HTTP and stores them in a time-series database (TSDB).
3. **Grafana** connects to Prometheus as a data source and renders interactive operational dashboards.

---

## 5. Implementation Blueprint in `mini-blog`

### A. Add Tracing & Metrics Dependencies (`pom.xml`)
```xml
<!-- Micrometer Tracing Bridge for Brave (Zipkin compatible) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>

<!-- Prometheus Metrics Exporter -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### B. Configure Tracing & Sampling (`application.yml`)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  tracing:
    sampling:
      probability: 1.0 # 100% sampling for development; set to 0.1 (10%) in Production
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

---

## 6. Trade-Off Analysis: Observability Tools Comparison

| Dimension | Zipkin / Jaeger | Prometheus + Grafana | ELK Stack / Grafana Loki |
| :--- | :--- | :--- | :--- |
| **Primary Category** | **Distributed Tracing** | **Metrics & Time-Series Monitoring** | **Log Aggregation** |
| **Data Format** | Spans, Timelines, Latency DAGs | Counter, Gauge, Histogram, Summary | Unstructured / Structured JSON logs |
| **Storage Engine** | In-Memory / Cassandra / Elasticsearch | Prometheus TSDB | Elasticsearch / Loki Chunk Storage |
| **Key Metric Answered** | *"Where is the latency bottleneck in this request chain?"* | *"What is the CPU usage and HTTP 5xx error rate?"* | *"What is the exact stack trace of this failure?"* |

---
*Stored at `docs/09-distributed-observability.md` for reference.*
