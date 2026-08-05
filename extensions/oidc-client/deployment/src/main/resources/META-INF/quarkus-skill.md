
### What This Extension Does

Programmatically acquires access tokens from OIDC/OAuth2 providers (Keycloak, Auth0, etc.) using grant types like `client_credentials` or `password`. Use it for service-to-service authentication when your app needs to call protected downstream APIs.

### Injecting OidcClient

```java
@Inject OidcClient oidcClient;

public String getAccessToken() {
    Tokens tokens = oidcClient.getTokens().await().indefinitely();
    return tokens.getAccessToken();
}
```

- `getTokens()` returns `Uni<Tokens>` — use `.await().indefinitely()` for blocking or chain reactively.
- `Tokens` provides: `getAccessToken()`, `getRefreshToken()`, `getAccessTokenExpiresAt()`.
- `getAccessTokenExpiresAt()` returns epoch seconds — compare with `System.currentTimeMillis() / 1000`.

### Configuration (Client Credentials Grant)

```properties
quarkus.oidc-client.auth-server-url=https://keycloak.example.com/realms/myrealm
quarkus.oidc-client.client-id=my-service
quarkus.oidc-client.credentials.secret=my-secret
quarkus.oidc-client.grant.type=client
```

- `grant.type=client` — client credentials grant (service-to-service, no user involved).
- Dev Services with Keycloak auto-configures `auth-server-url`, `client-id`, and `credentials.secret` — no manual config needed in dev/test.

### Token Refresh

```java
Tokens newTokens = oidcClient.refreshTokens(tokens.getRefreshToken())
    .await().indefinitely();
```

Not all grant types return refresh tokens. Client credentials typically don't — just call `getTokens()` again.

### Named Clients

Configure multiple OIDC clients for different providers/realms:

```properties
quarkus.oidc-client.service-a.auth-server-url=https://provider-a.example.com
quarkus.oidc-client.service-a.client-id=client-a
quarkus.oidc-client.service-a.credentials.secret=secret-a
quarkus.oidc-client.service-a.grant.type=client

quarkus.oidc-client.service-b.auth-server-url=https://provider-b.example.com
quarkus.oidc-client.service-b.client-id=client-b
quarkus.oidc-client.service-b.credentials.secret=secret-b
quarkus.oidc-client.service-b.grant.type=client
```

```java
@Inject OidcClients oidcClients;

OidcClient clientA = oidcClients.getClient("service-a");
Tokens tokens = clientA.getTokens().await().indefinitely();
```

### Using with REST Client

Add `quarkus-oidc-client-filter` to automatically inject tokens into REST client calls:

```java
@RegisterRestClient
@OidcClientFilter  // automatically adds Bearer token from default OidcClient
public interface DownstreamService {
    @GET @Path("/data")
    String getData();
}
```

For named clients: `@OidcClientFilter("service-a")`.

### Dev Services

Keycloak Dev Services starts automatically when `oidc-client` is present. It:
- Starts a Keycloak container
- Creates a realm with a service client
- Auto-configures `auth-server-url`, `client-id`, and `credentials.secret`
- No manual Keycloak setup needed in dev/test

### Testing

- Dev Services provides a real Keycloak — tokens are real JWTs.
- Test token acquisition: call `oidcClient.getTokens()` and verify the access token is non-null and non-empty.
- For downstream service testing, mock the service or use `@QuarkusTest` with the real Keycloak.

### Common Pitfalls

- `grant.type=client` is the default — you don't need to set it explicitly for client credentials.
- `Tokens.getAccessTokenExpiresAt()` returns epoch seconds, not a duration — compute remaining time with `expiresAt - (System.currentTimeMillis() / 1000)`.
- `OidcClient.getTokens()` is reactive (`Uni<Tokens>`) — block with `.await().indefinitely()` in imperative code.
- For REST client integration, add `quarkus-oidc-client-filter` as a separate extension — it's not included with `oidc-client`.
- Don't confuse `quarkus-oidc` (bearer token verification for incoming requests) with `quarkus-oidc-client` (token acquisition for outgoing requests).
