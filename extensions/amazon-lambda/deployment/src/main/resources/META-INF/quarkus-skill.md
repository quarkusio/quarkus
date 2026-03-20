### Lambda Handler

- Implement `com.amazonaws.services.lambda.runtime.RequestHandler<Input, Output>`.
- Mark with `@Named("myHandler")` if multiple handlers exist.
- The handler is a CDI bean — injection works.

### Configuration

- `quarkus.lambda.handler=myHandler` — select the active handler.

### Native Image

- Build native with `./mvnw package -Dnative` for fast cold starts.
- Use `manage.sh` script for deploying to AWS.

### Testing

- Use `@QuarkusTest` with `LambdaClient.invoke()` for local testing.

### Common Pitfalls

- Lambda functions should be stateless — do NOT store state in instance fields.
- Native builds require GraalVM.
