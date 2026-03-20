### Usage

- Add this extension to emit Quarkus-specific events to Java Flight Recorder (JFR).
- REST requests, security events, and other framework events are recorded automatically.
- Use JDK Mission Control (JMC) or `jfr` CLI tool to analyze recordings.

### Recording

- Start a recording with `jcmd <pid> JFR.start` or configure JVM args: `-XX:StartFlightRecording=filename=recording.jfr`.
- In dev mode, use the Dev UI to start/stop recordings.

### Testing

- JFR integration does not affect application behavior — no special test setup needed.

### Common Pitfalls

- JFR has minimal overhead and IS supported in native image builds — set `quarkus.native.monitoring=jfr`.
- For metrics export to external systems, use `quarkus-micrometer` or `quarkus-opentelemetry` instead.
