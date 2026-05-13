
### Usage

This extension integrates Keycloak Authorization Services with Quarkus OIDC for **centralized, policy-based authorization**. Permissions, resources, scopes, and policies are managed in Keycloak — not in application code. It works alongside `quarkus-oidc` — you need both.

### How It Works

The policy enforcer intercepts HTTP requests and checks with Keycloak whether the authenticated user has permission to access the requested resource. Authorization decisions are made in Keycloak based on configured policies (role-based, user-based, time-based, custom, etc.), not via annotations in code.

### Configuration

```properties
# OIDC configuration (required)
quarkus.oidc.auth-server-url=http://localhost:8180/realms/myrealm
quarkus.oidc.client-id=my-app
quarkus.oidc.credentials.secret=my-secret

# Enable policy enforcement
quarkus.keycloak.policy-enforcer.enable=true

# Path-specific configuration
quarkus.keycloak.policy-enforcer.paths.public.path=/api/public
quarkus.keycloak.policy-enforcer.paths.public.enforcement-mode=DISABLED

quarkus.keycloak.policy-enforcer.paths.admin.path=/api/admin/*
quarkus.keycloak.policy-enforcer.paths.admin.methods.get.method=GET
quarkus.keycloak.policy-enforcer.paths.admin.methods.get.scopes=admin:read
```

Endpoints don't need security annotations — the policy enforcer handles authorization based on Keycloak resource/scope mappings. Use `enforcement-mode=DISABLED` for public paths.

### Enforcement Modes

- `ENFORCING` (default) — all requests must have a valid token and matching permission in Keycloak
- `PERMISSIVE` — allows requests but logs policy decisions (useful for testing)
- `DISABLED` — no enforcement for the path

### Dev Services

Keycloak Dev Services starts a Keycloak container automatically. Import a pre-configured realm:

```properties
quarkus.keycloak.devservices.realm-path=dev-realm.json
```

Place the realm JSON export in `src/main/resources/`. The realm should include resources, scopes, policies, and permissions for your endpoints.

### Testing

```java
@QuarkusTest
class ProtectedResourceTest {

    @Test
    void publicEndpointAccessible() {
        given().when().get("/api/public").then().statusCode(200);
    }

    @Test
    void protectedEndpointRequiresAuth() {
        given().when().get("/api/admin").then().statusCode(401);
    }
}
```

Use Keycloak Dev Services with a realm that defines test users with appropriate permissions.

### Common Pitfalls

- **Requires `quarkus-oidc`**: This extension doesn't work alone — it adds authorization on top of OIDC authentication.
- **Policy enforcer disabled by default**: You must set `quarkus.keycloak.policy-enforcer.enable=true` to activate it.
- **Authorization is managed in Keycloak**: Resources, scopes, policies, and permissions must be configured in the Keycloak realm. The extension enforces them — it doesn't define them.
- **Dev Services realm import**: Without a realm JSON, Dev Services starts Keycloak with an empty realm. Pre-configure your realm and set `realm-path`.
- **Token propagation**: For service-to-service calls, use `quarkus-oidc-token-propagation` to forward tokens to downstream services.
