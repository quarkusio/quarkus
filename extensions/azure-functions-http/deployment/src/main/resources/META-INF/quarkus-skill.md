### Usage

- Add this extension to deploy Quarkus REST endpoints as Azure Functions using the HTTP trigger.
- Your existing REST endpoints work as-is — no code changes needed.
- Configure with Azure Functions Maven/Gradle plugin for deployment.

### Project Setup

- Use `quarkus create app --extension=azure-functions-http` to scaffold a project with the correct structure.
- The `host.json` and `function.json` files are generated automatically.

### Testing

- Use `@QuarkusTest` — test your REST endpoints normally with REST Assured.
- Azure Functions specifics are abstracted away at test time.

### Common Pitfalls

- Do NOT use WebSockets or server-sent events — Azure Functions HTTP trigger is request/response only.
- Cold start times can be significant — consider native image compilation for better startup.
