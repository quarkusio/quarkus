### Usage

- Add this extension to store OIDC session tokens in a database instead of cookies.
- Requires a datasource (JDBC) to be configured — tokens are stored in a database table.
- Useful when tokens are too large for cookies or when server-side session management is needed.

### Testing

- Use `@QuarkusTest` — Dev Services provides both Keycloak and database containers.

### Common Pitfalls

- Requires a JDBC datasource — add a JDBC driver extension (e.g., `quarkus-jdbc-postgresql`).
- Do NOT use alongside `quarkus-oidc-redis-token-state-manager` — choose one token storage backend.
