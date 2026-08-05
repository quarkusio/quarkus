
### WebAuthnUserProvider (Required)

You must implement `WebAuthnUserProvider` — there is no default. Only two methods are required:

```java
@ApplicationScoped
public class MyWebAuthnUserProvider implements WebAuthnUserProvider {

    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String username) {
        // return credentials for this user, or empty list if not found
    }

    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        // return the credential, or a failed Uni if not found
    }

    @Override
    public Set<String> getRoles(String username) {
        return Set.of("user"); // default returns empty set
    }
}
```

If using built-in endpoints, also override `store()` and `update()` (they default to no-ops).

### Persisting Credentials

Use `RequiredPersistedData` to convert between the internal representation and your storage:

```java
// After registration — extract fields to persist
WebAuthnCredentialRecord.RequiredPersistedData data = record.getRequiredPersistedData();
// Fields: username, credentialId (String), aaguid (UUID), publicKey (byte[]),
//         publicKeyAlgorithm (long), counter (long)
// Tip: data.getPublicKeyPEM() returns a PEM string if you prefer not storing byte[]

// When loading — reconstruct from persisted fields
WebAuthnCredentialRecord record = WebAuthnCredentialRecord.fromRequiredPersistedData(
    new RequiredPersistedData(username, credentialId, aaguid, publicKey, publicKeyAlgorithm, counter));
```

### Built-in Endpoints (Simplest Approach)

Enable them in configuration — they handle the full WebAuthn ceremony:

```properties
quarkus.webauthn.enable-registration-endpoint=true
quarkus.webauthn.enable-login-endpoint=true
```

Built-in paths (all under `/q/webauthn/`):
- `GET /q/webauthn/register-options-challenge?username=X` — returns registration challenge JSON
- `POST /q/webauthn/register?username=X` — completes registration (JSON body)
- `GET /q/webauthn/login-options-challenge?username=X` — returns login challenge JSON
- `POST /q/webauthn/login` — completes login (JSON body)
- `GET /q/webauthn/logout` — clears session, redirects to `/`

When using built-in endpoints, your `WebAuthnUserProvider.store()` and `update()` are called automatically.

### Manual Flow (Full Control)

Inject `WebAuthnSecurity` and handle registration/login yourself:

```java
@Inject WebAuthnSecurity security;
@Inject MyUserProvider userProvider;

@POST @Path("/register")
public Uni<String> register(@QueryParam("username") String username,
        @BeanParam WebAuthnRegisterResponse register, RoutingContext ctx) {
    return security.register(username, register, ctx)
        .map(record -> {
            userProvider.store(record);           // persist credentials yourself
            security.rememberUser(record.getUsername(), ctx); // set session cookie
            return "OK";
        });
}

@POST @Path("/login")
public Uni<String> login(@BeanParam WebAuthnLoginResponse login, RoutingContext ctx) {
    return security.login(login, ctx)
        .map(record -> {
            userProvider.update(record.getCredentialID(), record.getCounter());
            security.rememberUser(record.getUsername(), ctx);
            return "OK";
        });
}
```

Key methods on `WebAuthnSecurity`:
- `getRegisterChallenge(username, displayName, ctx)` — returns `Uni<PublicKeyCredentialCreationOptions>`
- `getLoginChallenge(username, ctx)` — returns `Uni<PublicKeyCredentialRequestOptions>`
- `register(username, response, ctx)` — verifies and returns `Uni<WebAuthnCredentialRecord>`
- `login(response, ctx)` — verifies and returns `Uni<WebAuthnCredentialRecord>`
- `rememberUser(username, ctx)` — sets the session cookie
- `logout(ctx)` — clears the session cookie

### Configuration

```properties
quarkus.webauthn.relying-party.name=My App
# quarkus.webauthn.relying-party.id defaults to the request host
# quarkus.webauthn.session-timeout=30m
# quarkus.webauthn.cookie-name=quarkus-credential
```

### Testing

Add the test dependency `quarkus-test-security-webauthn`. It provides:
- `WebAuthnHardware` — simulates a FIDO2 authenticator for tests
- `WebAuthnEndpointHelper` — helper methods for register/login flows with REST Assured
- `WebAuthnTestUserProvider` — in-memory user provider for tests

### Common Pitfalls

- **Must implement `WebAuthnUserProvider`** — the app won't start without exactly one `@ApplicationScoped` implementation.
- **Built-in endpoints are disabled by default** — set `enable-registration-endpoint` and `enable-login-endpoint` to `true` if you want them.
- **`findByCredentialId` must return a failed Uni on not found** — not a null-item Uni. This is how the framework distinguishes "credential exists" from "credential unknown".
- **`store()` must reject duplicate usernames** — otherwise attackers can add their credentials to existing accounts. Only allow adding credentials to the currently logged-in user.
- **Manual flow requires `rememberUser()`** — `register()` and `login()` verify credentials but don't set the session cookie. You must call `rememberUser()` yourself.
- **WebAuthn only works over HTTPS** (or localhost) — browsers reject the API on plain HTTP.
