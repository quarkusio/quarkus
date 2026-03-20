### Database-Backed Authentication

- Annotate an entity with `@UserDefinition`.
- Mark fields with `@Username`, `@Password`, `@Roles`.
- Passwords must be hashed — use `BcryptUtil.bcryptHash("password")`.

### Configuration

- Enable HTTP Basic: `quarkus.http.auth.basic=true`.
- Or use form-based auth with `quarkus.http.auth.form.enabled=true`.

### Testing

- Use `@TestSecurity(user = "alice", roles = "admin")` for simple tests.
- Or use REST Assured with basic auth for integration tests.

### Common Pitfalls

- Do NOT store plain-text passwords — always use `BcryptUtil`.
- The `@Roles` field can be a comma-separated string or a collection.
