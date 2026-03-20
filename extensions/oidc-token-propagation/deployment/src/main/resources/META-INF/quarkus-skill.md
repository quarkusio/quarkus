### Usage

- Add this extension to propagate the incoming OIDC bearer token to outgoing RESTEasy Classic REST client calls.
- Annotate your REST client interface with `@RegisterProvider(AccessTokenRequestFilter.class)`.
- The token from the incoming request is automatically forwarded — no manual token handling needed.

### Testing

- Use `@QuarkusTest` with Keycloak Dev Services.
- Test with an authenticated request that triggers a downstream REST client call.

### Common Pitfalls

- This propagates the EXISTING incoming token — it does not acquire a new token. For token acquisition, use `quarkus-oidc-client`.
- This is for RESTEasy Classic — for the reactive REST client, use `quarkus-oidc-token-propagation-reactive`.
