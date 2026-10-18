
### Securing Endpoints

```java
@Path("/api")
public class SecuredResource {

    @Inject JsonWebToken jwt;

    @GET @Path("/public")
    @PermitAll
    public String publicEndpoint() { return "Hello!"; }

    @GET @Path("/protected")
    @RolesAllowed("user")
    public String protectedEndpoint() {
        return "Hello, " + jwt.getName() + "!";  // getName() returns principal (from upn claim)
    }

    @GET @Path("/admin")
    @RolesAllowed("admin")
    public String adminEndpoint() { return "Admin: " + jwt.getName(); }
}
```

- `@RolesAllowed` checks the `groups` claim in the JWT.
- `@PermitAll` allows unauthenticated access.
- `@DenyAll` blocks all access.
- Without any annotation, the endpoint is public by default (unless `quarkus.http.auth.permission` is configured).

### JsonWebToken Claims

```java
@Inject JsonWebToken jwt;

jwt.getName()          // principal name (from upn, preferred_username, or sub)
jwt.getSubject()       // the "sub" claim — NOT auto-set by Jwt.upn()
jwt.getIssuer()        // the "iss" claim
jwt.getGroups()        // the "groups" claim (roles)
jwt.getExpirationTime()
jwt.getClaim("custom") // any custom claim
```

**Important**: `jwt.getName()` returns the principal (from `upn` claim). `jwt.getSubject()` returns the `sub` claim, which is null unless explicitly set with `Jwt.subject(...)`.

### Injecting Individual Claims

```java
@RequestScoped  // REQUIRED for individual claim injection
@Path("/api")
public class MyResource {
    @Claim(standard = Claims.sub) String subject;
    @Claim("tenant") String tenant;
    @Claim(standard = Claims.groups) Set<String> groups;
}
```

Individual `@Claim` injection requires `@RequestScoped` on the bean.

### Generating Tokens (smallrye-jwt-build)

Add the `quarkus-smallrye-jwt-build` extension:

```java
import io.smallrye.jwt.build.Jwt;

String token = Jwt.upn("alice")
    .groups(Set.of("user", "admin"))
    .issuer("https://example.com")
    .claim("tenant", "acme")
    .expiresIn(Duration.ofHours(1))
    .sign();
```

- The `issuer` in the token **must match** `mp.jwt.verify.issuer` — otherwise verification fails with 401.
- In dev/test mode, keys are auto-generated — no key configuration needed.

### Parsing Tokens Manually

```java
@Inject JWTParser parser;

JsonWebToken parsed = parser.parse(tokenString);
String sub = parsed.getSubject();
```

Throws `ParseException` for invalid or expired tokens.

### Configuration

```properties
# Required: issuer must match tokens
mp.jwt.verify.issuer=https://example.com

# Optional: explicit public key (not needed in dev/test — auto-generated)
# mp.jwt.verify.publickey.location=publicKey.pem
# smallrye.jwt.sign.key.location=privateKey.pem
```

### Auto-Generated Keys (Dev/Test)

In dev and test modes, Quarkus auto-generates an RSA 2048-bit key pair if no verification key is configured. The private key is available for `Jwt.sign()` and the public key for verification. No configuration needed.

### Testing

```java
@QuarkusTest
class SecuredResourceTest {
    @Test
    void testProtectedWithToken() {
        String token = Jwt.upn("alice")
            .groups("user")
            .issuer("https://example.com")  // must match mp.jwt.verify.issuer
            .sign();

        given()
            .header("Authorization", "Bearer " + token)
            .get("/api/protected")
            .then()
            .statusCode(200)
            .body(containsString("alice"));
    }

    @Test
    void testUnauthorized() {
        given().get("/api/protected").then().statusCode(401);
    }

    @Test
    void testForbidden() {
        String userToken = Jwt.upn("bob").groups("user")
            .issuer("https://example.com").sign();
        given()
            .header("Authorization", "Bearer " + userToken)
            .get("/api/admin")
            .then()
            .statusCode(403);
    }
}
```

### Common Pitfalls

- `jwt.getName()` returns the principal (from `upn`). `jwt.getSubject()` returns `sub` — these are different claims.
- Token issuer MUST match `mp.jwt.verify.issuer` — a mismatch causes silent 401 rejection.
- `@Claim` injection requires `@RequestScoped` on the enclosing bean.
- `Jwt.upn(name)` sets the `upn` claim, NOT the `sub` claim. Use `Jwt.subject(name)` to set `sub`.
- In tests, always set `.issuer(...)` when building tokens to match your config.
