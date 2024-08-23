package io.quarkus.grpc.auth;

import static io.quarkus.grpc.auth.GrpcAuthTestBase.AUTHORIZATION;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.inject.Singleton;

import io.grpc.Metadata;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;

@Singleton
public class BasicGrpcSecurityMechanism implements GrpcSecurityMechanism {
    @Override
    public boolean handles(Metadata metadata) {
        String authString = metadata.get(AUTHORIZATION);
        return authString != null && authString.startsWith("Basic ");
    }

    @Override
    public AuthenticationRequest createAuthenticationRequest(Metadata metadata) {
        String authString = metadata.get(AUTHORIZATION);
        authString = authString.substring("Basic ".length());
        byte[] decode = Base64.getDecoder().decode(authString);
        String plainChallenge = new String(decode, StandardCharsets.UTF_8);
        int colonPos;
        if ((colonPos = plainChallenge.indexOf(':')) > -1) {
            String userName = plainChallenge.substring(0, colonPos);
            char[] password = plainChallenge.substring(colonPos + 1).toCharArray();
            return new UsernamePasswordAuthenticationRequest(userName, new PasswordCredential(password));
        } else {
            return null;
        }
    }
}
