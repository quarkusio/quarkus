package io.quarkus.it.keycloak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.jwt.build.Jwt;

@Singleton
public class ClientAuthWithSignedJwtCreator {

    private final KeyPair keyPair;
    private final Path signedJwtTokenPath;
    private volatile ClientMetadata createdClientMetadata = null;

    ClientAuthWithSignedJwtCreator() {
        this.keyPair = generateRsaKeyPair();
        this.signedJwtTokenPath = Path.of("target").resolve("signed-jwt-token");
    }

    /**
     * This observer creates client that use authentication with signed JWT, signs a JWT token and stores it as a file.
     * This token is valid for 5 minutes and can be used for: https://datatracker.ietf.org/doc/html/rfc7523#section-2.2
     * Quarkus will get the token from the file and use it to get tokens from the token endpoint.
     */
    void observe(@Observes StartupEvent event, OidcClientRegistration clientRegistration,
            @ConfigProperty(name = "keycloak.url") String keycloakUrl) {
        generateRsaKeyPair();
        var requestClientMetadata = createClientMetadata();
        var registeredClient = clientRegistration.registerClient(requestClientMetadata).await().indefinitely();
        this.createdClientMetadata = registeredClient.metadata();
        var signedJwt = createSignedJwt(keycloakUrl, this.createdClientMetadata.getClientId());
        Log.debugf("Client 'signed-jwt-test' has signed JWT token %s", signedJwt);
        storeSignedJwtToken(signedJwt);
    }

    ClientMetadata getCreatedClientMetadata() {
        return createdClientMetadata;
    }

    Path getSignedJwtTokenPath() {
        return signedJwtTokenPath;
    }

    private void storeSignedJwtToken(String signedJwt) {
        try {
            Files.writeString(signedJwtTokenPath, signedJwt);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create signed JWT token", e);
        }
    }

    private String createSignedJwt(String keycloakUrl, String clientId) {
        return Jwt.preferredUserName("alice")
                .groups("Contributor")
                .issuer(clientId)
                .audience(keycloakUrl + "/realms/quarkus/protocol/openid-connect/token")
                .subject(clientId)
                .sign(keyPair.getPrivate());
    }

    private ClientMetadata createClientMetadata() {
        return ClientMetadata.builder()
                .redirectUri("http://localhost:8081/protected/jwt-bearer-token-file")
                .tokenEndpointAuthMethod("private_key_jwt")
                .clientName("signed-jwt-test")
                .jwk(keyPair.getPublic())
                .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }
}
