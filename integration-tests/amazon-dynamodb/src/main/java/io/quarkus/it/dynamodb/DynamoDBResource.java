package io.quarkus.it.dynamodb;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@Path("/")
public class DynamoDBResource {
    private final static String ASYNC_TABLE = "async";
    private final static String BLOCKING_TABLE = "blocking";

    private static final Logger LOG = Logger.getLogger(DynamoDBResource.class);

    @ConfigProperty(name = "dynamodb.port", defaultValue = "8000")
    String dynamoDbPort;

    @ConfigProperty(name = "dynamodb.aws", defaultValue = "false")
    Boolean useAwsAccount;

    private DynamoDbAsyncClient asyncClient;

    private DynamoDbClient client;

    void onStart(@Observes StartupEvent ev) {
        if (useAwsAccount) {
            asyncClient = DynamoDbAsyncClient.create();
            client = DynamoDbClient.create();
        } else {
            asyncClient = DynamoDbAsyncClient.builder()
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                    .region(Region.of("localhost"))
                    .endpointOverride(URI.create("http://localhost:" + dynamoDbPort))
                    .build();

            client = DynamoDbClient.builder()
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                    .region(Region.of("localhost"))
                    .endpointOverride(URI.create("http://localhost:" + dynamoDbPort))
                    .build();
        }
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async client with table: " + ASYNC_TABLE);
        String keyValue = UUID.randomUUID().toString();
        String rangeValue = UUID.randomUUID().toString();

        return DynamoDBUtils.createTableIfNotExistsAsync(asyncClient, ASYNC_TABLE)
                .thenCompose(t -> asyncClient.putItem(DynamoDBUtils.createPutRequest(ASYNC_TABLE, keyValue, rangeValue, "OK")))
                .thenCompose(p -> asyncClient.getItem(DynamoDBUtils.createGetRequest(ASYNC_TABLE, keyValue, rangeValue)))
                .thenApply(p -> p.item().get(DynamoDBUtils.PAYLOAD_NAME).s());
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlocking() {
        LOG.info("Testing Blocking client with table: " + BLOCKING_TABLE);

        String keyValue = UUID.randomUUID().toString();
        String rangeValue = UUID.randomUUID().toString();
        GetItemResponse item = null;

        if (DynamoDBUtils.createTableIfNotExists(client, BLOCKING_TABLE)) {
            if (client.putItem(DynamoDBUtils.createPutRequest(BLOCKING_TABLE, keyValue, rangeValue, "OK")) != null) {
                item = client.getItem(DynamoDBUtils.createGetRequest(BLOCKING_TABLE, keyValue, rangeValue));
            }
        }

        if (item != null) {
            return item.item().get(DynamoDBUtils.PAYLOAD_NAME).s();
        } else {
            return "ERROR";
        }
    }

}
