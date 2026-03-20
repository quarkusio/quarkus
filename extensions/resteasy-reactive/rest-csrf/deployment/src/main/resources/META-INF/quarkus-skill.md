### Usage

- Add this extension for Cross-Site Request Forgery (CSRF) prevention on REST endpoints.
- CSRF tokens are automatically generated and validated for form-based submissions.
- Configure the cookie and header names with `quarkus.rest-csrf.*` properties.

### Integration with Qute

- In Qute templates, use `{inject:csrf.token}` to include the CSRF token in forms.

### Testing

- Use `@QuarkusTest` with REST Assured — include the CSRF token in test requests.
- Fetch the CSRF cookie first, then include it as a form field or header in subsequent requests.

### Common Pitfalls

- CSRF protection applies to state-changing methods (POST, PUT, DELETE) — GET requests are not protected.
- For REST APIs consumed by JavaScript SPAs, ensure the CSRF token is sent as a header.
