### Usage

- Add this extension to automatically propagate OIDC tokens from RESTEasy Classic REST clients.
- Annotate your REST client interface with `@RegisterProvider(OidcClientRequestFilter.class)`.
- Requires `quarkus-oidc-client` for token acquisition.

### Testing

- Use `@QuarkusTest` with Keycloak Dev Services for integration testing.
- Mock the OIDC client in unit tests if needed.

### Common Pitfalls

- This is for RESTEasy Classic — for the reactive REST client, use `quarkus-oidc-client-reactive-filter` instead.
- Do NOT use this for incoming token validation — use `quarkus-oidc` for that.
