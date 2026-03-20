### Usage

- Add this extension to trigger Funqy functions from Knative Events (CloudEvents).
- Functions annotated with `@Funq` are invoked when matching CloudEvents are received.
- Map CloudEvent types to functions with `quarkus.funqy.knative-events.mapping.<function>.trigger`.

### Testing

- Use `@QuarkusTest` with REST Assured — send CloudEvent-formatted HTTP requests.
- Include `Ce-Type`, `Ce-Source`, `Ce-Id` headers in test requests.

### Common Pitfalls

- Ensure CloudEvent type mappings are configured — without them, Funqy won't know which function to invoke.
- For HTTP-only functions, use `quarkus-funqy-http` instead.
