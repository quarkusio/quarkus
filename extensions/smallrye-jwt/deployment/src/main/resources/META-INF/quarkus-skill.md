### JWT Token Validation

- Validates bearer tokens from the `Authorization` header automatically.
- Use `@RolesAllowed` with roles from the JWT `groups` claim.

### Configuration

- `mp.jwt.verify.publickey.location` — public key or JWKS URL for token verification.
- `mp.jwt.verify.issuer` — expected token issuer.
- `smallrye.jwt.path.groups` — claim path for roles (default: `groups`).

### Claim Injection

- Inject claims with `@Claim("email") String email`.
- Inject the full token with `@Inject JsonWebToken jwt`.

### Testing

- Use `@TestSecurity(user = "alice", roles = "user")` to simulate JWT auth.
- Or generate test tokens with `smallrye-jwt-build` extension.

### Common Pitfalls

- Token validation requires a configured public key — without it, all requests fail.
- The `groups` claim path varies by OIDC provider — configure `smallrye.jwt.path.groups`.
