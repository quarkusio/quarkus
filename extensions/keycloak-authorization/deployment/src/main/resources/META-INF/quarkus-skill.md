### Policy Enforcement

- Automatically enforces Keycloak authorization policies on REST endpoints.
- `quarkus.keycloak.policy-enforcer.enable=true` — enable policy enforcement.

### Configuration

- Policies are managed in Keycloak — define resources, scopes, and permissions there.
- `quarkus.keycloak.policy-enforcer.paths./api/*\.methods.GET.method=GET` — path-level policies.

### Testing

- Use `@TestSecurity` with roles matching Keycloak policies.
- Use Keycloak Dev Services for integration tests.

### Common Pitfalls

- Requires Keycloak as the OIDC provider — not compatible with other providers.
- Policy definitions must exist in Keycloak before enforcement works.
