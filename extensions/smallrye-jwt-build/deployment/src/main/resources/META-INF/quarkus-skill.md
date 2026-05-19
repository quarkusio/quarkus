
### Generating Signed JWTs

Use the fluent `Jwt` builder from `io.smallrye.jwt.build.Jwt`:

```java
String token = Jwt.upn("alice")
    .groups(Set.of("User", "Admin"))
    .issuer("https://example.com")
    .expiresIn(Duration.ofHours(1))
    .sign();
```

Common claim methods:
- `Jwt.upn("username")` — sets the `upn` (User Principal Name) claim
- `Jwt.subject("sub")` — sets the `sub` claim
- `Jwt.preferredUserName("name")` — sets `preferred_username`
- `Jwt.groups(Set.of(...))` — sets `groups` claim (used for `@RolesAllowed`)
- `Jwt.claim("key", value)` — sets any custom claim
- `Jwt.issuer("url")` — sets the `iss` claim (overrides config)
- `Jwt.expiresIn(Duration)` or `Jwt.expiresAt(Instant)` — controls expiration

### Dev Mode Key Generation

In dev/test mode, Quarkus auto-generates an RSA key pair — **no key configuration needed**. The generated keys are set as:
- `mp.jwt.verify.publickey` — for verification
- `smallrye.jwt.sign.key` — for signing

Keys regenerate on each app restart.

### Production Key Configuration

For production, provide your own keys:

```properties
# Signing key (private)
smallrye.jwt.sign.key.location=privateKey.pem

# Verification key (public) — if also verifying tokens in this app
mp.jwt.verify.publickey.location=publicKey.pem
```

Place key files in `src/main/resources/`.

### Token Configuration

```properties
# Issuer — must match mp.jwt.verify.issuer if verifying in the same app
smallrye.jwt.new-token.issuer=https://example.com
mp.jwt.verify.issuer=https://example.com

# Token lifespan in seconds (default: 300 = 5 minutes)
smallrye.jwt.new-token.lifespan=3600
```

### Verifying Tokens in Tests

Inject `JWTParser` (from `smallrye-jwt`) to parse and verify tokens:

```java
@Inject JWTParser jwtParser;

@Test
void verifyToken() throws Exception {
    String token = Jwt.upn("alice").groups(Set.of("User")).sign();
    JsonWebToken jwt = jwtParser.parse(token);
    assertEquals("alice", jwt.getName());
    assertTrue(jwt.getGroups().contains("User"));
}
```

### Encryption (JWE)

Encrypt a JWT instead of (or in addition to) signing:

```java
// Encrypt only
String jwe = Jwt.upn("alice").jwe().encrypt();

// Sign then encrypt (inner-sign)
String signedAndEncrypted = Jwt.upn("alice").innerSign().encrypt();
```

### Common Pitfalls

- **Issuer mismatch**: `smallrye.jwt.new-token.issuer` and `mp.jwt.verify.issuer` must match if you both generate and verify tokens in the same app. Mismatches cause silent verification failures.
- **Default lifespan is 300 seconds** (5 minutes) — set `smallrye.jwt.new-token.lifespan` for longer-lived tokens.
- **`sign()` uses the configured private key** — in dev mode this is auto-generated. In production you must provide `smallrye.jwt.sign.key.location`.
- **`Jwt` is a static entry point**, not an injectable bean — use `Jwt.claims()` or `Jwt.upn()` directly, no `@Inject` needed.
