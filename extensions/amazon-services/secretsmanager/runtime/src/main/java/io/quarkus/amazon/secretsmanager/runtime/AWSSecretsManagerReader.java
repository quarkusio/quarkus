package io.quarkus.amazon.secretsmanager.runtime;

import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

@ApplicationScoped
public class AWSSecretsManagerReader {

    @Inject
    SecretsManagerClient client;

    public String getSecret(final String key) throws Exception {
        final GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest
                .builder()
                .secretId(key)
                .build();
        GetSecretValueResponse secretValueResponse = client.getSecretValue(getSecretValueRequest);

        String secret;
        if (secretValueResponse.secretString() != null) {
            secret = secretValueResponse.secretString();
        } else {
            secret = new String(Base64.getMimeDecoder().decode(secretValueResponse.secretBinary().asByteArray()));
        }
        return secret;
    }
}
