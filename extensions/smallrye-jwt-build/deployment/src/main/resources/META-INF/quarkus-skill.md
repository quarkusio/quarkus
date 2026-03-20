### Usage

- Add this extension to create and sign JWT tokens programmatically.
- Use `Jwt.claims()` builder API to construct tokens.
- Supports RSA, EC, and symmetric key signing.

### Pattern

```java
String token = Jwt.issuer("https://my-issuer.com")
    .subject("user@example.com")
    .groups(Set.of("admin", "user"))
    .expiresIn(Duration.ofHours(1))
    .sign();
```

### Key Configuration

- Configure the signing key: `smallrye.jwt.sign.key.location=privateKey.pem`.
- For HMAC signing: `smallrye.jwt.sign.key.location=secret.key`.

### Testing

- Generate tokens in tests to authenticate test requests.
- Use with `@QuarkusTest` and REST Assured: set the token in the `Authorization: Bearer <token>` header.

### Common Pitfalls

- This extension is for TOKEN CREATION only — for token verification, use `quarkus-smallrye-jwt`.
- Do NOT hardcode signing keys in source code — use configuration properties or files.
