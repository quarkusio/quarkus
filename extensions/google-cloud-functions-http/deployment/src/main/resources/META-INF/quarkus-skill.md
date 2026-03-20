### Usage

- Add this extension to deploy Quarkus REST endpoints as Google Cloud Functions using the HTTP trigger.
- Your existing REST endpoints work as-is — no code changes needed.
- Deploy using `gcloud functions deploy`.

### Testing

- Use `@QuarkusTest` — test your REST endpoints normally with REST Assured.

### Common Pitfalls

- Do NOT use WebSockets or server-sent events — Cloud Functions HTTP is request/response only.
- Cold start times can be significant — consider native image for better startup.
- For non-HTTP triggers, use `quarkus-google-cloud-functions` or `quarkus-funqy-google-cloud-functions`.
