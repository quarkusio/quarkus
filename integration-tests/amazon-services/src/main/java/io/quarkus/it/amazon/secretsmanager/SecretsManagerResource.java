package io.quarkus.it.amazon.secretsmanager;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Path("/secretsmanager")
public class SecretsManagerResource {

    private static final Logger LOG = Logger.getLogger(SecretsManagerResource.class);
    public final static String TEXT = "Quarkus is awsome";
    private static final String SYNC_PARAM = "quarkus/sync-" + UUID.randomUUID().toString();
    private static final String ASYNC_PARAM = "quarkus/async-" + UUID.randomUUID().toString();

    @Inject
    SecretsManagerClient secretsManagerClient;

    @Inject
    SecretsManagerAsyncClient secretsManagerAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync Secrets Manager client with secret name: " + SYNC_PARAM);
        //Put parameter
        secretsManagerClient.createSecret(r -> r.name(SYNC_PARAM).secretString(TEXT));
        //Get parameter
        return secretsManagerClient.getSecretValue(r -> r.secretId(SYNC_PARAM)).secretString();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async SSM client with parameter: " + ASYNC_PARAM);
        //Put and get parameter
        return secretsManagerAsyncClient.createSecret(r -> r.name(ASYNC_PARAM).secretString(TEXT))
                .thenCompose(result -> secretsManagerAsyncClient.getSecretValue(r -> r.secretId(ASYNC_PARAM)))
                .thenApply(GetSecretValueResponse::secretString);
    }
}
