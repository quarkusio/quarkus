### Structured JSON Logging

- Enables JSON-formatted log output for log aggregation (ELK, Splunk, CloudWatch).
- Enabled by adding this extension — no code changes needed.

### Configuration

- `quarkus.log.console.json=true` (default when extension is present).
- `quarkus.log.console.json.additional-field.app.value=my-app` — add custom fields.
- `quarkus.log.console.json.date-format=default` — timestamp format.

### Common Pitfalls

- JSON logging is enabled for console output only — file handlers need separate config.
- Disable in dev for readability: `%dev.quarkus.log.console.json=false`.
