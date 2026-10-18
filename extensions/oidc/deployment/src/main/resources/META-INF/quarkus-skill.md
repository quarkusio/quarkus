### Application Types

- `service` (default) — bearer token validation for APIs. Tokens validated but no login flow.
- `web-app` — authorization code flow with login redirect. For server-rendered apps.
- `hybrid` — supports both. Set `quarkus.oidc.application-type`.

### Configuration

- Set `quarkus.oidc.auth-server-url` to the OIDC provider (e.g. Keycloak realm URL).
- For `service`: set auth-server-url only. Token validation is automatic.
- For `web-app`: also set `quarkus.oidc.client-id` and `quarkus.oidc.credentials.secret`.

### Token Access

- Inject the **access** token with `@Inject JsonWebToken jwt` (no qualifier needed).
- Inject the **ID** token with `@Inject @IdToken JsonWebToken idToken`.
- Access claims with `idToken.getClaim("email")` or `idToken.getName()`.
- Use `@RolesAllowed` with roles from the token for authorization.

### Keycloak Dev Services

- When Keycloak is not configured, Dev Services starts a Keycloak container automatically.
- A default realm with test users is created. Check Dev UI for credentials.

### Testing

- Add `quarkus-test-security-oidc` as a test dependency for OIDC-specific test annotations.
- Use `@TestSecurity(user = "alice", roles = "user")` for simple auth simulation.
- For custom token claims, combine `@TestSecurity` with `@OidcSecurity(claims = @Claim(key = "email", value = "alice@example.com"))`.
- For integration tests with real tokens, use `OidcTestClient`.

### Common Pitfalls

- Do NOT hardcode auth-server-url — use `%prod.` prefix and let Dev Services handle dev/test.
- Token validation requires the OIDC server to be reachable — handle startup failures gracefully.
