package io.quarkus.it.dynamodb;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Path("/")
public class DynamoDBResource {
    private final static String KEY_NAME = "keyId";
    private final static String RANGE_NAME = "rangeId";
    private final static String PAYLOAD_NAME = "payload";

    private static final Logger LOG = Logger.getLogger(DynamoDBResource.class);

    @ConfigProperty(name = "dynamodb.port", defaultValue = "8000")
    String dynamoDbPort;

    private DynamoDbAsyncClient asyncClient;

    private DynamoDbClient client;

    void onStart(@Observes StartupEvent ev) {
        asyncClient = DynamoDbAsyncClient.builder()
                .region(Region.of("localhost"))
                .endpointOverride(URI.create("http://localhost:" + dynamoDbPort))
                .build();

        client = DynamoDbClient.builder()
                .region(Region.of("localhost"))
                .endpointOverride(URI.create("http://localhost:" + dynamoDbPort))
                .build();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync(@QueryParam("table") String table) {
        LOG.info("Testing Async with table: " + table);
        return asyncClient.createTable(createTableRequest(table))
                .thenCompose(t -> asyncClient.putItem(createPutRequest(table, "key1", "range1", "OK")))
                .thenCompose(p -> asyncClient.getItem(createGetRequest(table, "key1", "range1")))
                .thenApply(p -> p.item().get(PAYLOAD_NAME).s());
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlocking(@QueryParam("table") String table) {
        LOG.info("Testing Blocking with table: " + table);
        GetItemResponse item = null;
        if (client.createTable(createTableRequest(table)) != null) {
            if (client.putItem(createPutRequest(table, "key1", "range1", "OK")) != null) {
                item = client.getItem(createGetRequest(table, "key1", "range1"));
            }
        }

        if (item != null) {
            return item.item().get(PAYLOAD_NAME).s();
        } else {
            return "ERROR";
        }
    }

    private CreateTableRequest createTableRequest(String table) {
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions
                .add(AttributeDefinition.builder().attributeName(KEY_NAME).attributeType(ScalarAttributeType.S).build());
        attributeDefinitions.add(
                AttributeDefinition.builder().attributeName(RANGE_NAME).attributeType(ScalarAttributeType.S).build());

        List<KeySchemaElement> ks = new ArrayList<>();
        ks.add(KeySchemaElement.builder().attributeName(KEY_NAME).keyType(KeyType.HASH).build());
        ks.add(KeySchemaElement.builder().attributeName(RANGE_NAME).keyType(KeyType.RANGE).build());

        ProvisionedThroughput provisionedthroughput = ProvisionedThroughput.builder().readCapacityUnits(1000L)
                .writeCapacityUnits(1000L).build();

        return CreateTableRequest.builder()
                .tableName(table)
                .attributeDefinitions(attributeDefinitions)
                .keySchema(ks)
                .provisionedThroughput(provisionedthroughput)
                .build();
    }

    private PutItemRequest createPutRequest(String table, String keyValue, String rangeValue, String payLoad) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(KEY_NAME, AttributeValue.builder().s(keyValue).build());
        item.put(RANGE_NAME, AttributeValue.builder().s(rangeValue).build());
        item.put(PAYLOAD_NAME, AttributeValue.builder().s(payLoad).build());

        return PutItemRequest.builder()
                .tableName(table)
                .item(item)
                .build();
    }

    private GetItemRequest createGetRequest(String table, String keyValue, String rangeValue) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(KEY_NAME, AttributeValue.builder().s(keyValue).build());
        key.put(RANGE_NAME, AttributeValue.builder().s(rangeValue).build());

        return GetItemRequest.builder()
                .tableName(table)
                .key(key)
                .attributesToGet(PAYLOAD_NAME)
                .build();
    }
}
