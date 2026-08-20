# Phase 6: High-Performance Inter-Service Communication (gRPC & Protobuf vs REST Feign)

## 1. Architectural Motivation (Why gRPC for Microservices?)

Most microservices applications start by using RESTful APIs over HTTP/1.1 with JSON payloads for all inter-service communication (e.g., using Spring Cloud OpenFeign). While REST/JSON is universal, human-readable, and easy to debug, it exhibits significant performance bottlenecks in high-throughput internal microservices interactions:

### Limitations of REST/JSON Over HTTP/1.1:
1. **JSON Payload Overhead**: Text-based JSON contains verbose repetitive keys (`"id": "123"`, `"username": "john"`) and string formatting overhead, requiring heavy CPU cycles for serialization/deserialization.
2. **HTTP/1.1 Head-of-Line (HOL) Blocking**: Each HTTP/1.1 request requires a separate TCP connection or must wait in a sequential queue on a persistent connection.
3. **Verbose Headers**: Plaintext HTTP headers sent with every request consume unnecessary bandwidth.
4. **Lack of Strict Contract Enforcement**: REST APIs lack strict, compile-time enforced schemas across different language boundaries.

### The Solution: gRPC & Protocol Buffers
**gRPC** is an open-source, high-performance Remote Procedure Call (RPC) framework developed by Google. It leverages **HTTP/2** for transport and **Protocol Buffers (Protobuf)** as its binary serialization wire format.

---

## 2. Under-the-Hood Mechanics

### A. HTTP/1.1 vs HTTP/2 Transport Layer

| Feature | HTTP/1.1 (Standard REST) | HTTP/2 (gRPC) |
| :--- | :--- | :--- |
| **Connection Usage** | 1 TCP connection per concurrent request (or connection pooling) | **Multiplexing**: Hundreds of concurrent requests/responses over a single shared TCP connection |
| **Head-of-Line Blocking** | High (Requests blocked if TCP packet lost or response delayed) | Solved at stream layer (Multiplexed independent binary streams) |
| **Header Handling** | Plaintext ASCII headers repeated on every request | **HPACK Compression**: Huffman coding and header indexing tables |
| **Communication Flow** | Request-Response only | Unary, Server Streaming, Client Streaming, Bidirectional Streaming |

```
HTTP/1.1 Connection Pool:
[TCP Conn 1] ---> Request A -----------------> Response A
[TCP Conn 2] ---> Request B -----------------> Response B

HTTP/2 Multiplexing:
[Single Shared TCP Conn] ===> [Stream 1: Req A] [Stream 3: Req B] [Stream 1: Resp A] [Stream 3: Resp B]
```

### B. JSON vs Protocol Buffers (Protobuf) Binary Wire Format

Protobuf encodes data into a compact binary format using **Tag-Length-Value (TLV)** encoding and **Varints** (variable-length integers):

- **JSON Payload (64 bytes)**:
  `{"id": 1001, "username": "alice", "email": "alice@example.com"}`
- **Protobuf Binary Payload (18 bytes)**:
  Protobuf strips field names entirely! It replaces text keys with numeric field tags (`1`, `2`, `3`) defined in the `.proto` schema:
  `08-ED-07-12-05-61-6C-69-63-65-1A-11-61-6C-69-63-65-40...`

**Result**: Protobuf payloads are typically **5x to 10x smaller** than JSON, and serialization is **5x to 8x faster**, dramatically reducing CPU usage and network latency.

---

## 3. Dual-Communication Strategy in `mini-blog`

To balance agility with performance, we implement a **Hybrid Communication Architecture**:

```
[Mobile / Web Client] 
        |
        | HTTP/1.1 REST (JSON)
        v
  [API Gateway] 
        |
        | OpenFeign REST + Resilience4j (Flexible / External Integration)
        +-----------------------------> [identity-service]
        |
        v
  [post-service]
        |
        | High-Performance gRPC over HTTP/2 (Binary Protobuf)
        +-----------------------------> [profile-service]
```

1. **REST via OpenFeign**: Used for external client endpoints, authentication flows (`identity-service`), and low-frequency operations where human readability and flexible REST tooling are beneficial.
2. **gRPC via Protobuf**: Used for high-frequency, low-latency internal service-to-service calls (e.g., `post-service` fetching author profile metadata from `profile-service` during post feed generation).

---

## 4. Protobuf Schema & Implementation Blueprint

### A. Define Protobuf Contract (`profile.proto`)
Create `src/main/proto/profile.proto` in `profile-service` and `post-service`:

```protobuf
syntax = "proto3";

package com.htphatz.profile;

option java_multiple_files = true;
option java_package = "com.htphatz.profile.grpc";

service ProfileGrpcService {
  rpc GetProfileById (ProfileRequest) returns (ProfileResponse);
}

message ProfileRequest {
  string user_id = 1;
}

message ProfileResponse {
  string user_id = 1;
  string username = 2;
  string avatar_url = 3;
  string bio = 4;
}
```

### B. gRPC Server Implementation (`profile-service`)
```java
@GrpcService
public class ProfileGrpcServiceImpl extends ProfileGrpcServiceGrpc.ProfileGrpcServiceImplBase {
    
    @Autowired
    private ProfileRepository profileRepository;

    @Override
    public void getProfileById(ProfileRequest request, StreamObserver<ProfileResponse> responseObserver) {
        var profile = profileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND));

        ProfileResponse response = ProfileResponse.newBuilder()
                .setUserId(profile.getUserId())
                .setUsername(profile.getUsername())
                .setAvatarUrl(profile.getAvatarUrl())
                .setBio(profile.getBio())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

### C. gRPC Client Call (`post-service`)
```java
@Service
public class PostService {

    @GrpcClient("profile-service")
    private ProfileGrpcServiceGrpc.ProfileGrpcServiceBlockingStub profileStub;

    public PostResponse getPostWithAuthor(String postId) {
        // Fetch post from MongoDB...
        ProfileResponse author = profileStub.getProfileById(
                ProfileRequest.newBuilder().setUserId(post.getAuthorId()).build()
        );
        return assemblePostResponse(post, author);
    }
}
```

---

## 5. Trade-Off Analysis: REST Feign vs gRPC

| Dimension | REST via OpenFeign | gRPC via Protobuf |
| :--- | :--- | :--- |
| **Transport Protocol** | HTTP/1.1 | HTTP/2 |
| **Payload Format** | Plaintext JSON | Compact Binary Protobuf |
| **CPU Overhead** | High (JSON parsing) | Low (Binary decoding) |
| **Human Readability** | High (Readable in Postman/Curl) | Low (Requires Protobuf decoder) |
| **Streaming Support** | Limited (Server-Sent Events) | Full (Unary, Client, Server, Bi-directional) |
| **Best Use Case** | Public APIs, Web/Mobile Clients, CRUD | Internal Microservices, Real-time High Throughput |

---
*Stored at `docs/06-grpc-vs-rest-feign.md` for reference.*
