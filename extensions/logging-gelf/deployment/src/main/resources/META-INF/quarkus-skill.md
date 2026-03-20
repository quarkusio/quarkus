### Usage

- Add this extension to send log messages in GELF (Graylog Extended Log Format) to a centralized logging system.
- Supports Graylog, Logstash, and any GELF-compatible log aggregator.
- Configure with `quarkus.log.handler.gelf.enabled=true` and `quarkus.log.handler.gelf.host=<server>`.

### Configuration

- Set `quarkus.log.handler.gelf.port` (default: 12201 for UDP).
- Add extra fields with `quarkus.log.handler.gelf.additional-field.<name>.value=<value>`.
- Configure log level with `quarkus.log.handler.gelf.level=INFO`.

### Testing

- GELF logging is additive — console logging still works in tests.
- Use `@QuarkusTest` normally — disable GELF in test profile if not needed: `%test.quarkus.log.handler.gelf.enabled=false`.

### Common Pitfalls

- Do NOT enable GELF in dev/test mode without a running GELF server — it will cause connection errors.
- For structured JSON logging to stdout, use `quarkus-logging-json` instead.
