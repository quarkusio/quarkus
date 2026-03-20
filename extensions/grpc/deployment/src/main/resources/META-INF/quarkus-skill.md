### Proto Files

- Place `.proto` files in `src/main/proto/`.
- Quarkus generates Java classes at build time — do NOT edit generated code.

### gRPC Server

- Implement the generated `*Grpc.*ImplBase` and annotate with `@GrpcService`.
- Methods receive request and return `Uni<Response>` (Mutiny) or use `StreamObserver`.

### gRPC Client

- Inject with `@GrpcClient("my-service") MyServiceGrpc.MyServiceBlockingStub client`.
- Configure: `quarkus.grpc.clients.my-service.host=localhost`.

### Dev Services

- gRPC server starts on a separate port (default 9000) in dev mode.

### Testing

- Use `@QuarkusTest` — inject `@GrpcClient` in tests to call server methods.

### Common Pitfalls

- Proto files MUST be in `src/main/proto` — other locations are not scanned by default.
- gRPC uses HTTP/2 — ensure clients and load balancers support it.
