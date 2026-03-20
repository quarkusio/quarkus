### Usage

- Add this extension to automatically propagate OIDC tokens from REST Client (reactive) calls.
- Annotate your REST client interface with `@RegisterProvider(OidcClientRequestReactiveFilter.class)`.
- Requires `quarkus-oidc-client` for token acquisition.

### Testing

- Use `@QuarkusTest` with Keycloak Dev Services for integration testing.

### Common Pitfalls

- This is for the reactive REST Client — for RESTEasy Classic, use `quarkus-oidc-client-filter` instead.
- Do NOT use this for incoming token validation — use `quarkus-oidc` for that.
