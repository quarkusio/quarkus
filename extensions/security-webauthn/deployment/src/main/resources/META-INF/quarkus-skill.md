### Passwordless Authentication

- WebAuthn enables biometric/hardware key authentication.
- Inject `WebAuthnSecurity` for registration and login flows.

### Entity Storage

- Implement `WebAuthnUserProvider` to store credentials in your database.
- Store `WebAuthnCredentialRecord` entities.

### Configuration

- `quarkus.webauthn.relying-party-name=MyApp` — display name.
- `quarkus.webauthn.origin=https://myapp.com` — allowed origin.

### Common Pitfalls

- WebAuthn requires HTTPS in production (localhost is exempt for development).
- Browser support varies — always provide a password fallback.
