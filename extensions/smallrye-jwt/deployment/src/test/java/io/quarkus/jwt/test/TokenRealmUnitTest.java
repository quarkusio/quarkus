package io.quarkus.jwt.test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.AnonymousIdentityProvider;
import io.quarkus.security.runtime.QuarkusIdentityProviderManagerImpl;
import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;

/**
 * Validate usage of the bearer token based realm
 */
public class TokenRealmUnitTest {

    @Test
    public void testAuthenticator() throws Exception {
        KeyPair keyPair = generateKeyPair();
        PublicKey pk1 = keyPair.getPublic();
        PrivateKey pk1Priv = keyPair.getPrivate();
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo((RSAPublicKey) pk1, "https://server.example.com");
        MpJwtValidator jwtValidator = new MpJwtValidator(new DefaultJWTParser(contextInfo), null);
        QuarkusIdentityProviderManagerImpl authenticator = QuarkusIdentityProviderManagerImpl.builder()
                .addProvider(new AnonymousIdentityProvider())
                .setBlockingExecutor(new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                })
                .addProvider(jwtValidator).build();

        String jwt = TokenUtils.generateTokenString("/Token1.json", pk1Priv, "testTokenRealm");
        TokenAuthenticationRequest tokenEvidence = new TokenAuthenticationRequest(new TokenCredential(jwt, "bearer"));
        SecurityIdentity securityIdentity = authenticator.authenticate(tokenEvidence).await().indefinitely();
        Assertions.assertNotNull(securityIdentity);
        Assertions.assertEquals("jdoe@example.com", securityIdentity.getPrincipal().getName());
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048); // because that's the minimal accepted size
        return generator.generateKeyPair();
    }
}
