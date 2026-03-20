### Usage

- Add this extension to deploy Funqy functions as AWS Lambda functions.
- Annotate methods with `@Funq` — they are automatically exposed as Lambda handlers.
- Each function takes one input and returns one output.

### Deployment

- Build with `./mvnw package` — produces `target/function.zip`.
- Deploy using SAM CLI or AWS CDK.
- Set the Lambda handler to `io.quarkus.funqy.lambda.FunqyStreamHandler::handleRequest`.

### Testing

- Use `@QuarkusTest` — Funqy functions can be tested via HTTP in dev/test mode.

### Common Pitfalls

- Only ONE `@Funq` function can be deployed per Lambda — if you have multiple, specify which with `quarkus.funqy.export`.
- For REST APIs on Lambda, prefer `quarkus-amazon-lambda-http` — it supports full REST routing.
