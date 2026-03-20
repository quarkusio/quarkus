### Usage

- Add this extension for OpenID Connect Dynamic Client Registration (RFC 7591).
- Allows your application to register as an OIDC client dynamically at the identity provider.
- Configure with `quarkus.oidc-client-registration.auth-server-url` and registration metadata.

### Testing

- Use `@QuarkusTest` with Keycloak Dev Services — dynamic registration is supported by Keycloak.

### Common Pitfalls

- Not all identity providers support dynamic client registration — verify provider support first.
- Do NOT confuse this with `quarkus-oidc-client` — this extension registers new clients, while `oidc-client` acquires tokens for existing clients.
