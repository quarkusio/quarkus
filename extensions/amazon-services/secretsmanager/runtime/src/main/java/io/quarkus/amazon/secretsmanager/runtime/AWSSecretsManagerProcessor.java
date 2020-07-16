package io.quarkus.amazon.secretsmanager.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@ApplicationScoped
public class AWSSecretsManagerProcessor {

    @Inject
    SecretsManagerClient client;

    @Produces
    @AWSSecretsManager("")
    String getStringValue(InjectionPoint ip) {
        return ip.getAnnotated().getAnnotation(AWSSecretsManager.class).value();
    }

    //    private String getSecret(final String key) {
    //        final GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest
    //                .builder()
    //                .secretId(key)
    //                .build();
    //        GetSecretValueResponse secretValueResponse;
    //        try {
    //            secretValueResponse = client.getSecretValue(getSecretValueRequest);
    //        } catch (DecryptionFailureException e) {
    //            throw new RuntimeException("Secrets Manager can't decrypt.");
    //        } catch (InternalServiceErrorException e) {
    //            throw new RuntimeException("An error occurred on the server side.");
    //        } catch (InvalidRequestException e) {
    //            throw new RuntimeException(
    //                    "You provided a parameter value that is not valid for the current state of the resource.");
    //        } catch (ResourceNotFoundException e) {
    //            throw new RuntimeException("We can't find the resource that you asked for: " + key);
    //        }
    //        return secretValueResponse.secretString();
    //    }
}
