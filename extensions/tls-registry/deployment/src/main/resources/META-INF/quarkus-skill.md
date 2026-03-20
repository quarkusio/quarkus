### TLS Configuration

- Define named TLS configurations for use by other extensions.
- `quarkus.tls."my-tls".key-store.pem.0.cert=cert.pem` — PEM certificate.
- `quarkus.tls."my-tls".key-store.pem.0.key=key.pem` — PEM key.
- Supports PEM, JKS, and PKCS12 formats.

### mTLS

- Configure trust store for mutual TLS: `quarkus.tls."my-tls".trust-store.pem.certs=ca.pem`.

### Usage by Other Extensions

- Reference TLS config: `quarkus.http.tls-configuration-name=my-tls`.
- Works with HTTP server, REST client, gRPC, Kafka, etc.

### Common Pitfalls

- Certificate paths are relative to the working directory — use absolute paths in production.
- Let's Encrypt integration requires periodic certificate renewal.
