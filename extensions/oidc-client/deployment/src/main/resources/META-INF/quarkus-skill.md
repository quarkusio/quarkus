### Token Acquisition

- Inject `OidcClient` to acquire tokens programmatically.
- Common flow: client credentials grant for service-to-service auth.

### Configuration

- `quarkus.oidc-client.auth-server-url` — OIDC provider URL.
- `quarkus.oidc-client.client-id` and `quarkus.oidc-client.credentials.secret`.
- `quarkus.oidc-client.grant.type=client` — for client credentials flow.

### Token Propagation

- Use with REST Client: register `OidcClientRequestFilter` via `@RegisterProvider(OidcClientRequestFilter.class)` on the REST client interface.
- Or use the `quarkus-oidc-client-filter` extension for simplified setup.

### Common Pitfalls

- Tokens are cached and refreshed automatically — do NOT manage token lifecycle manually.
