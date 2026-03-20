### Usage

- Add this extension to propagate the incoming OIDC bearer token to outgoing REST Client (reactive) calls.
- Annotate your REST client interface with `@RegisterProvider(AccessTokenRequestReactiveFilter.class)`.
- The token from the incoming request is automatically forwarded.

### Testing

- Use `@QuarkusTest` with Keycloak Dev Services.

### Common Pitfalls

- This propagates the EXISTING incoming token — for acquiring new tokens, use `quarkus-oidc-client`.
- This is for the reactive REST Client — for RESTEasy Classic, use `quarkus-oidc-token-propagation`.
