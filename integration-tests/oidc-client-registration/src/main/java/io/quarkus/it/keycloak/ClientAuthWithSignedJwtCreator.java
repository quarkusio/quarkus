package io.quarkus.it.keycloak;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.base64url.Base64Url;

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
        var requestClientMetadata = new ClientMetadata(createClientMetadataJson(keycloakUrl));
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

    private String createClientMetadataJson(String keycloakUrl) {
        RSAPublicKey rsaKey = (RSAPublicKey) keyPair.getPublic();
        String modulus = Base64Url.encode(toIntegerBytes(rsaKey.getModulus()));
        String publicExponent = Base64Url.encode(toIntegerBytes(rsaKey.getPublicExponent()));
        return """
                {
                  "redirect_uris" : [ "http://localhost:8081/protected/jwt-bearer-token-file" ],
                  "token_endpoint_auth_method" : "private_key_jwt",
                  "grant_types" : [ "client_credentials", "authorization_code" ],
                  "client_name" : "signed-jwt-test",
                  "client_uri" : "%1$s/auth/realms/quarkus/app",
                  "jwks" : {
                    "keys" : [ {
                      "kid" : "%4$s",
                      "kty" : "RSA",
                      "alg" : "RS256",
                      "use" : "sig",
                      "e" : "%3$s",
                      "n" : "%2$s"
                    } ]
                  }
                }
                """
                .formatted(keycloakUrl, modulus, publicExponent, createKeyId());
    }

    private String createKeyId() {
        try {
            return Base64Url.encode(MessageDigest.getInstance("SHA-256").digest(keyPair.getPrivate().getEncoded()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate key id", e);
        }
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

    private static byte[] toIntegerBytes(final BigInteger bigInt) {
        final int bitlen = bigInt.bitLength();
        // following code comes from the Keycloak project

        final int bytelen = (bitlen + 7) / 8;
        final byte[] array = bigInt.toByteArray();
        if (array.length == bytelen) {
            // expected number of bytes, return them
            return array;
        } else if (bytelen < array.length) {
            // if array is greater is because the sign bit (it can be only 1 byte more), remove it
            return Arrays.copyOfRange(array, array.length - bytelen, array.length);
        } else {
            // if array is smaller fill it with zeros
            final byte[] resizedBytes = new byte[bytelen];
            System.arraycopy(array, 0, resizedBytes, bytelen - array.length, array.length);
            return resizedBytes;
        }
    }

}
