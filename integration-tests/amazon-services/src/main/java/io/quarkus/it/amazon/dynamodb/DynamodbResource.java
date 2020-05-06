package io.quarkus.it.amazon.dynamodb;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@Path("/dynamodb")
public class DynamodbResource {
    private final static String ASYNC_TABLE = "async";
    private final static String BLOCKING_TABLE = "blocking";

    private static final Logger LOG = Logger.getLogger(DynamodbResource.class);

    @Inject
    DynamoDbClient dynamoClient;

    @Inject
    DynamoDbAsyncClient dynamoAsyncClient;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() {
        LOG.info("Testing Async Dynamodb client with table: " + ASYNC_TABLE);
        String keyValue = UUID.randomUUID().toString();
        String rangeValue = UUID.randomUUID().toString();

        return DynamoDBUtils.createTableIfNotExistsAsync(dynamoAsyncClient, ASYNC_TABLE)
                .thenCompose(
                        t -> dynamoAsyncClient.putItem(DynamoDBUtils.createPutRequest(ASYNC_TABLE, keyValue, rangeValue, "OK")))
                .thenCompose(p -> dynamoAsyncClient.getItem(DynamoDBUtils.createGetRequest(ASYNC_TABLE, keyValue, rangeValue)))
                .thenApply(p -> p.item().get(DynamoDBUtils.PAYLOAD_NAME).s())
                .exceptionally(th -> {
                    LOG.error("Error during async Dynamodb operations", th.getCause());
                    return "ERROR";
                });
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlockingDynamo() {
        LOG.info("Testing Blocking Dynamodb client with table: " + BLOCKING_TABLE);

        String keyValue = UUID.randomUUID().toString();
        String rangeValue = UUID.randomUUID().toString();
        GetItemResponse item = null;

        if (DynamoDBUtils.createTableIfNotExists(dynamoClient, BLOCKING_TABLE)) {
            if (dynamoClient.putItem(DynamoDBUtils.createPutRequest(BLOCKING_TABLE, keyValue, rangeValue, "OK")) != null) {
                item = dynamoClient.getItem(DynamoDBUtils.createGetRequest(BLOCKING_TABLE, keyValue, rangeValue));
            }
        }

        if (item != null) {
            return item.item().get(DynamoDBUtils.PAYLOAD_NAME).s();
        } else {
            return "ERROR";
        }
    }
}
