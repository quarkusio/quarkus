### Authorization Annotations

- `@RolesAllowed("admin")` — restrict to specific roles.
- `@Authenticated` — require any authenticated user.
- `@PermitAll` — allow unauthenticated access (override class-level restrictions).
- `@DenyAll` — deny all access.
- Place annotations on class (applies to all methods) or individual methods.

### SecurityIdentity

- Inject `SecurityIdentity` to access the current user's principal, roles, and attributes.
- Use `identity.getPrincipal().getName()` for the username.
- Use `identity.hasRole("admin")` for programmatic role checks.

### Proactive Authentication

- By default, authentication is proactive (happens before endpoint execution).
- Set `quarkus.http.auth.proactive=false` for lazy authentication (only when needed).

### HTTP Basic Auth

- Enable with `quarkus.http.auth.basic=true`.
- Combine with a user store: embedded (`quarkus.security.users.embedded`), JPA, or LDAP.

### Testing

- Use `@TestSecurity(user = "alice", roles = "admin")` to simulate authenticated users in tests.
- Test both authorized and unauthorized access paths.
- No real identity provider needed in tests.

### Common Pitfalls

- Do NOT rely only on URL-based security — always use annotations on endpoints.
- `@RolesAllowed` requires an identity provider to be configured (e.g. OIDC, HTTP Basic auth, or a JPA identity store via `quarkus-security-jpa`).
- Annotations are checked at the CDI level — they do NOT work on non-CDI classes.
