package io.quarkus.it.dynamodb;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@Path("/")
public class DynamoDBResource {
    private final static String ASYNC_TABLE = "async";
    private final static String BLOCKING_TABLE = "blocking";

    private static final Logger LOG = Logger.getLogger(DynamoDBResource.class);

    @Inject
    DynamoDbClient client;

    // Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the
    // async support.
    //    @Inject
    //    DynamoDbAsyncClient asyncClient;

    // Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the
    // async support.
    //    @GET
    //    @Path("async")
    //    @Produces(TEXT_PLAIN)
    //    public CompletionStage<String> testAsync() {
    //        LOG.info("Testing Async client with table: " + ASYNC_TABLE);
    //        String keyValue = UUID.randomUUID().toString();
    //        String rangeValue = UUID.randomUUID().toString();
    //
    //        return DynamoDBUtils.createTableIfNotExistsAsync(asyncClient, ASYNC_TABLE)
    //                .thenCompose(t -> asyncClient.putItem(DynamoDBUtils.createPutRequest(ASYNC_TABLE, keyValue, rangeValue, "OK")))
    //                .thenCompose(p -> asyncClient.getItem(DynamoDBUtils.createGetRequest(ASYNC_TABLE, keyValue, rangeValue)))
    //                .thenApply(p -> p.item().get(DynamoDBUtils.PAYLOAD_NAME).s());
    //    }

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
