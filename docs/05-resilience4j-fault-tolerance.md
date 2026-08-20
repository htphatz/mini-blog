# Phase 5: Resilience & Fault Tolerance (Resilience4j)

## 1. Architectural Motivation (Why Fault Tolerance?)

In a monolithic application, method calls between components happen in-process on the same stack. If a method fails, it throws an exception handled locally.

In a **Microservices Architecture**, service interactions depend on network calls over HTTP/gRPC. Networks are inherently unreliable—subject to latency spikes, packet loss, transient network blips, and downstream service outages.

### The Danger of Cascading Failures:
Consider a call chain: `Client` -> `API Gateway` -> `post-service` -> `profile-service`.

1. If `profile-service` experiences a database lock or high CPU usage, responses slow down from **50ms to 30 seconds**.
2. Threads in `post-service` calling `profile-service` become blocked waiting for HTTP response timeouts.
3. Incoming requests to `post-service` continue to arrive, quickly consuming all available Tomcat/Jetty worker threads (e.g., max 200 threads).
4. `post-service` runs out of threads, becomes unresponsive, and fails.
5. `API Gateway` threads calling `post-service` now get blocked.
6. **Cascading Failure**: A minor latency issue in 1 downstream service crashes the entire microservices ecosystem.

---

## 2. Under-the-Hood Mechanics

Resilience4j is a lightweight, fault-tolerance library designed for Java 17/21 and functional programming, built around high-order functions and state machines.

### A. Circuit Breaker State Machine

The Circuit Breaker prevents a service from repeatedly executing an operation that is bound to fail, protecting downstream services and allowing them time to recover.

```
                    +-------------------------------------+
                    |               CLOSED                |
                    | (Normal Operation, Metrics Logged)  |
                    +-------------------------------------+
                                  |         ^
          Failure Rate > 50%      |         | Success Rate > Threshold
          or Slow Calls > 50%     |         | (During Test Phase)
                                  v         |
                    +-------------------------------------+
                    |                OPEN                 |
                    | (All Calls Instantly Rejected /     |
                    |  Fallback Triggered Immediately)    |
                    +-------------------------------------+
                                  |
                                  | Wait Duration Expires (e.g., 10s)
                                  v
                    +-------------------------------------+
                    |              HALF_OPEN              |
                    | (Permits N Test Calls to Evaluate)  |
                    +-------------------------------------+
                                  |
                                  | Failure Rate > Threshold
                                  +---------------------------> OPEN State
```

1. **CLOSED State**: Normal operation. Requests flow through. Metrics (failures, slow calls) are recorded in a **Sliding Window**.
2. **Sliding Window Types**:
   - **Count-based Sliding Window**: Aggregates metrics over the last $N$ requests (e.g., last 100 calls).
   - **Time-based Sliding Window**: Aggregates metrics over the last $N$ seconds (e.g., last 60 seconds).
3. **OPEN State**: When failure rate (e.g., >50%) or slow call rate (e.g., latency > 2s) exceeds configured thresholds, the breaker trips to `OPEN`. All incoming calls immediately fail fast (triggering Fallback logic) without making any network call.
4. **HALF_OPEN State**: After a configurable `waitDurationInOpenState` (e.g., 10 seconds), the breaker transitions to `HALF_OPEN` and permits a limited number of test requests. If test requests succeed, it resets to `CLOSED`; if they fail, it trips back to `OPEN`.

### B. Exponential Backoff Retry with Randomized Jitter

Retrying a failed network call immediately can overwhelm a recovering service (**Thundering Herd Problem**).

- **Exponential Backoff Formula**:
  $$\text{Wait Interval} = \text{initialInterval} \times \text{multiplier}^{\text{attempt}}$$
- **Randomized Jitter**: Adds a random variance to the backoff interval:
  $$\text{Interval with Jitter} = \text{Wait Interval} \pm \text{Random Variance}$$
  This spreads out retry requests from multiple client instances, preventing synchronized traffic spikes.

### C. Bulkhead Pattern (Resource Isolation)

Inspired by the watertight bulkheads of ships that prevent a single hull breach from sinking the entire vessel:

- **Semaphore Bulkhead**: Limits the max number of concurrent calls to a downstream service using atomic semaphores. Extremely low overhead.
- **ThreadPool Bulkhead**: Assigns a dedicated thread pool and bounded queue to each downstream service. If `profile-service` pool fills up, it does not affect `post-service` calls to `notification-service`.

---

## 3. Implementation Blueprint with OpenFeign & Resilience4j

### A. Add Dependencies (`post-service` `pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### B. Configure Resilience4j (`application.yml`)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      profileServiceCB:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 2000ms
        waitDurationInOpenState: 10000ms
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      profileServiceRetry:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

### C. OpenFeign Client with Fallback Factory
```java
@FeignClient(
    name = "profile-service",
    fallbackFactory = ProfileClientFallbackFactory.class
)
public interface ProfileClient {
    @GetMapping("/internal/users/{id}")
    ProfileResponse getProfileById(@PathVariable("id") String id);
}

@Component
@Slf4j
public class ProfileClientFallbackFactory implements FallbackFactory<ProfileClient> {
    @Override
    public ProfileClient create(Throwable cause) {
        log.error("ProfileClient fallback triggered due to: {}", cause.getMessage());
        return id -> ProfileResponse.builder()
                .id(id)
                .username("User Unavailable")
                .bio("Profile service is currently experiencing technical difficulties.")
                .build();
    }
}
```

---

## 4. Trade-Off Analysis: Resilience4j vs Hystrix vs Sentinel

| Feature | Resilience4j | Netflix Hystrix (Deprecated) | Alibaba Sentinel |
| :--- | :--- | :--- | :--- |
| **Maintenance Status** | Active (Recommended standard for Spring 3.x) | Maintenance mode / End of life | Active |
| **Programming Paradigm** | Functional, Lambda-friendly, RxJava/Project Reactor | OOP / Command Pattern | Flow Control / Rule-based |
| **Dependencies** | Zero external dependencies (Vavr lightweight) | Heavy dependencies | Moderate dependencies |
| **System Overhead** | Ultra-lightweight | Moderate (Thread context switching) | Low |

---
*Stored at `docs/05-resilience4j-fault-tolerance.md` for reference.*
