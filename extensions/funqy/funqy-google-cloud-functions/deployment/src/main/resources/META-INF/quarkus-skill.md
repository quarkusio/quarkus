### Usage

- Add this extension to deploy Funqy functions as Google Cloud Functions.
- Annotate methods with `@Funq` — they are exposed as Cloud Function handlers.
- Supports HTTP triggers and background function triggers (Pub/Sub, Storage, etc.).

### Deployment

- Build with `./mvnw package`.
- Deploy with `gcloud functions deploy`.

### Testing

- Use `@QuarkusTest` — Funqy functions can be tested via HTTP in dev/test mode.

### Common Pitfalls

- Only ONE `@Funq` function can be deployed per Cloud Function — specify with `quarkus.funqy.export` if multiple exist.
- For REST APIs, prefer `quarkus-google-cloud-functions-http`.
