
### Usage

Add the extension — JSON logging is enabled automatically for console output. No code changes needed.

Log output changes from:
```
2024-01-15 10:30:00 INFO  [com.example] Hello World
```
To:
```json
{"timestamp":"2024-01-15T10:30:00Z","level":"INFO","loggerName":"com.example","message":"Hello World","threadName":"main"}
```

### Configuration

```properties
# Disable JSON logging (e.g., keep plain text in dev mode)
quarkus.log.console.json=false

# Pretty-print JSON (readable but larger)
quarkus.log.console.json.pretty-print=true

# Add exception details as structured fields
quarkus.log.console.json.exception-output-type=formatted

# Customize timestamp format
quarkus.log.console.json.date-format=yyyy-MM-dd'T'HH:mm:ss.SSSZ

# Add static fields to every log entry
quarkus.log.console.json.additional-field.app.value=my-service
quarkus.log.console.json.additional-field.env.value=${ENVIRONMENT:dev}
```

### Dev Mode

Consider disabling JSON in dev mode for readability:

```properties
%dev.quarkus.log.console.json=false
```

### Structured Exception Logging

By default, exceptions are included as a formatted string. Options:
- `formatted` — stack trace as a string field
- `detailed` — structured with class, message, and frames

### Common Pitfalls

- **Enabled by default**: Adding the extension immediately changes all console output to JSON. If this breaks log parsing in dev, use `%dev.quarkus.log.console.json=false`.
- **File logging**: JSON format only applies to console by default. For file logging in JSON, configure `quarkus.log.file.json=true` separately.
- **Additional fields are static**: `additional-field` values are set at startup. For dynamic fields per-request, use MDC (Mapped Diagnostic Context).
- **Log level filtering still applies**: JSON formatting doesn't change which messages are logged — standard `quarkus.log.level` and category-level settings still apply.
