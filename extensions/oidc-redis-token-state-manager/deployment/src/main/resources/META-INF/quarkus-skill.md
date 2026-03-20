### Usage

- Add this extension to store OIDC session tokens in Redis instead of cookies.
- Requires `quarkus-redis-client` to be configured.
- Useful when tokens are too large for cookies or when server-side session management is needed.

### Testing

- Use `@QuarkusTest` — Dev Services provides both Keycloak and Redis containers.

### Common Pitfalls

- Requires `quarkus-redis-client` — add it as a dependency.
- Do NOT use alongside `quarkus-oidc-db-token-state-manager` — choose one token storage backend.
