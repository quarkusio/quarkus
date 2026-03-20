### Spring Security Compatibility

- `@Secured("ROLE_ADMIN")` — mapped to `@RolesAllowed("admin")`.
- `@PreAuthorize("hasRole('admin')")` — basic SpEL expressions supported.
- Works with Quarkus security identity providers.

### Testing

- Use `@TestSecurity(user = "alice", roles = "admin")` — same as Quarkus security tests.

### Common Pitfalls

- Only basic SpEL expressions are supported — complex Spring Security configurations may not work.
- Consider using Quarkus security annotations for new projects.
