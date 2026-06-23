
### Proto Files

Place `.proto` files in `src/main/proto/`. They are compiled automatically during the build — no plugin configuration needed.

```protobuf
syntax = "proto3";
package com.example;
option java_package = "com.example.grpc";

service GreetingService {
  rpc SayHello (HelloRequest) returns (HelloReply);
  rpc SayHelloToAll (stream HelloRequest) returns (stream HelloReply);
}
message HelloRequest { string name = 1; }
message HelloReply { string message = 1; }
```

### Generated Class Naming

For a service named `FooService`:
- **Server base**: `MutinyFooServiceGrpc.FooServiceImplBase` — extend this
- **Client stub**: `MutinyFooServiceGrpc.MutinyFooServiceStub` — inject this
- **Messages**: Generated as standard protobuf Java classes (`FooRequest`, `FooReply`)

### Implementing a Service

```java
@GrpcService
public class GreetingServiceImpl extends MutinyGreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(
            HelloReply.newBuilder().setMessage("Hello " + request.getName() + "!").build()
        );
    }

    @Override
    public Multi<HelloReply> sayHelloToAll(Multi<HelloRequest> requests) {
        return requests.map(req ->
            HelloReply.newBuilder().setMessage("Hello " + req.getName() + "!").build()
        );
    }
}
```

- Annotate with `@GrpcService` — makes it a CDI bean automatically.
- Implement the **Mutiny** variant (`MutinyFooServiceGrpc.FooServiceImplBase`), not the plain gRPC one.
- Unary RPCs return `Uni<Response>`. Streaming RPCs use `Multi<T>`.

### Consuming a Service (Client)

Inject the client stub with `@GrpcClient`:

```java
@GrpcClient("greeting")
MutinyGreetingServiceGrpc.MutinyGreetingServiceStub client;

public Uni<String> hello(String name) {
    return client.sayHello(HelloRequest.newBuilder().setName(name).build())
        .onItem().transform(HelloReply::getMessage);
}
```

Configure the client in `application.properties`:
```properties
quarkus.grpc.clients.greeting.host=localhost
```

For in-process calls (same app), no host config is needed — it auto-discovers the service.

### Error Handling

Return gRPC status codes for errors:

```java
throw new StatusRuntimeException(
    Status.NOT_FOUND.withDescription("Item not found: " + id));
```

### Interceptors

Server interceptor applied globally:

```java
@GlobalInterceptor
@ApplicationScoped
public class LoggingInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Log.infof("gRPC call: %s", call.getMethodDescriptor().getFullMethodName());
        return next.startCall(call, headers);
    }
}
```

Use `@RegisterInterceptor` on a service to apply an interceptor to a specific service only.

### Configuration

```properties
# Server
quarkus.grpc.server.port=9000              # default
quarkus.grpc.server.test-port=9001         # default in test mode

# Client
quarkus.grpc.clients.<name>.host=localhost
quarkus.grpc.clients.<name>.port=9000
quarkus.grpc.clients.<name>.plain-text=true  # needed for non-TLS
```

### Testing

- gRPC server runs on port 9001 in test mode (not the HTTP port).
- Inject clients in tests with `@GrpcClient`:
  ```java
  @GrpcClient("greeting")
  MutinyGreetingServiceGrpc.MutinyGreetingServiceStub client;

  @Test
  void testSayHello() {
      HelloReply reply = client.sayHello(
          HelloRequest.newBuilder().setName("World").build()
      ).await().atMost(Duration.ofSeconds(5));
      assertEquals("Hello World!", reply.getMessage());
  }
  ```
- If testing alongside REST, set `quarkus.http.test-port=0` to avoid port conflicts.

### Common Pitfalls

- Proto files must be in `src/main/proto/` — other locations are not auto-compiled.
- Use the **Mutiny** variant (`MutinyFooServiceGrpc`), not the plain gRPC variant — Quarkus generates both but expects Mutiny.
- `@GrpcClient("name")` name must match the config key in `quarkus.grpc.clients.<name>`.
- For in-process testing (client calling service in same app), the client auto-discovers the service — no host/port config needed.
- gRPC uses port 9000/9001 (test), not the HTTP port 8080/8081.
