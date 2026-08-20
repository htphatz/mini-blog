# Phase 8: Distributed Caching & Performance Optimization (Redis)

## 1. Architectural Motivation (Why Distributed Caching?)

In a microservices application, read operations typically outnumber write operations by a ratio of **10:1 to 100:1**. 

### The Problem With Direct Database Reads:
1. **Disk I/O Latency**: Fetching data from relational (MySQL), document (MongoDB), or graph (Neo4j) databases incurs disk I/O, index scans, and network round-trips, yielding response times of **20ms to 500ms**.
2. **Database CPU Exhaustion**: Repeatedly executing expensive queries (e.g., fetching popular blog posts or user profile metadata) consumes database connection pools and CPU cycles, degrading performance for concurrent write transactions.

### The Solution: Redis Distributed Cache
**Redis** (Remote Dictionary Server) is an in-memory, key-value data store operating at sub-millisecond latencies (**< 1ms**). A shared Redis cluster acts as a high-speed caching layer accessible across all microservice instances.

---

## 2. Under-the-Hood Mechanics

### A. Caching Patterns

```
                               +-------------------+
                               |   Client Request  |
                               +-------------------+
                                         |
                                         v
                               +-------------------+
                               |   Redis Cache     |
                               +-------------------+
                                 /               \
                          Cache Hit               Cache Miss
                         (Fast: <1ms)            (Read DB: ~50ms)
                            /                         \
                           v                           v
              +------------------+           +------------------+
              | Return Data Fast |           | Query Database   |
              +------------------+           +------------------+
                                                       |
                                            Update Cache + Return
```

1. **Cache-Aside (Lazy Loading)**:
   - Application checks Redis first.
   - **Cache Hit**: Returns data directly from Redis.
   - **Cache Miss**: Application queries the primary database, populates the result in Redis with a TTL, and returns the response to the user.
2. **Write-Through**: Application writes data to Redis and Database simultaneously in a single transaction.
3. **Write-Behind (Write-Back)**: Application writes to Redis immediately; an asynchronous worker flushes changes to the database in batches (Ultra-high throughput, risk of data loss if Redis crashes).

---

## 3. Mitigating Distributed Caching Vulnerabilities

### A. Cache Avalanche (Sập Thảm Họa Cache)
- **Problem**: Hundreds of cached entries (e.g., all top blog posts) are initialized at the same time with identical TTLs (e.g., exactly 1 hour). When the TTL expires, all requests suddenly hit the database at once, causing a crash.
- **Mitigation Strategy**: **Randomized TTL Jitter**. Add a random variance (e.g., 60 minutes + random 1-10 minutes) to key expiration times to stagger cache invalidation.

### B. Cache Penetration (Xuyên Thấu Cache)
- **Problem**: An attacker repeatedly requests non-existent IDs (e.g., `GET /api/v1/post/invalid-uuid-999`). Since the data never exists in DB or Redis, every request bypasses the cache and queries the DB directly.
- **Mitigation Strategies**:
  1. **Cache Null Values**: Store empty/null placeholders in Redis with a short TTL (e.g., 2 minutes) for missing keys.
  2. **Bloom Filters**: Use a probabilistic Bloom Filter in Redis to instantly determine if an ID *definitely does not exist* before querying the DB.

### C. Cache Stampede / Dogpiling (Trâu Điên Dẫm Đạp Cache)
- **Problem**: A super hot key (e.g., viral news post) expires. Within milliseconds, 5,000 concurrent requests detect a cache miss and all execute the exact same heavy SQL query simultaneously.
- **Mitigation Strategy**: **Distributed Locking (Mutex)**. The first request acquires a Redis Distributed Lock (`SET key value NX PX 5000`), queries the DB, updates the cache, and releases the lock. Subsequent requests wait for the lock or fetch the updated cache.

---

## 4. Implementation Blueprint in `mini-blog`

### A. Add Dependencies (`post-service` `pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### B. Configure Spring Cache Redis Manager (`RedisConfig.java`)
```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)) // Default TTL 15 mins
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("popular-posts", config.entryTtl(Duration.ofHours(1)))
                .build();
    }
}
```

### C. Annotate Service Methods (`PostService.java`)
```java
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    // Cache-Aside: Returns cached result if present, otherwise executes query & caches
    @Cacheable(value = "posts", key = "#postId")
    public PostResponse getPostById(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return mapToResponse(post);
    }

    // Invalidate Cache upon Update
    @CacheEvict(value = "posts", key = "#postId")
    public PostResponse updatePost(String postId, PostUpdateRequest request) {
        // Update post in MongoDB...
    }
}
```

---

## 5. Trade-Off Analysis: Redis vs Memcached vs Local Cache (Caffeine)

| Dimension | Local In-Memory Cache (Caffeine) | Redis Distributed Cache | Memcached |
| :--- | :--- | :--- | :--- |
| **Storage Location** | Inside JVM heap memory | Standalone external memory cluster | Standalone external memory cluster |
| **Data Consistency** | Inconsistent across instances | Centralized single source of truth | Centralized single source of truth |
| **Data Structures** | Key-Value Java Objects | Rich structures (Strings, Hashes, Lists, Sets, Sorted Sets, Bitmaps) | Simple Key-Value Strings |
| **Persistence** | Ephemeral (Lost on JVM restart) | RDB Snapshots & AOF Log Persistence | Ephemeral (Memory only) |
| **Latency** | Ultra-fast (< 100 nanoseconds) | Fast (< 1 millisecond) | Fast (< 1 millisecond) |

---
*Stored at `docs/08-redis-distributed-caching.md` for reference.*
