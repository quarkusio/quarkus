### Usage

- Add this extension to write Azure Functions with direct access to Azure Functions SDK bindings.
- Use `@HttpTrigger`, `@BlobTrigger`, `@QueueTrigger`, etc., from the Azure Functions Java SDK.
- For simple HTTP REST endpoints, prefer `quarkus-azure-functions-http` instead.

### Testing

- Use `@QuarkusTest` for integration testing.
- Azure Functions bindings can be tested with mock triggers.

### Common Pitfalls

- This extension gives raw Azure Functions SDK access — for REST endpoints, `quarkus-azure-functions-http` is simpler.
- Ensure the Azure Functions Maven/Gradle plugin is configured for deployment.
