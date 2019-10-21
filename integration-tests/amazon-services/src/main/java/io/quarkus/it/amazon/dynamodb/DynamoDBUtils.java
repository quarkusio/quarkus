package io.quarkus.it.amazon.dynamodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

public class DynamoDBUtils {
    private static final Logger LOG = Logger.getLogger(DynamoDBUtils.class);

    private final static String KEY_NAME = "keyId";
    private final static String RANGE_NAME = "rangeId";
    public final static String PAYLOAD_NAME = "payload";

    private static final int DEFAULT_WAIT_TIMEOUT = 10 * 60 * 1000; //10 minutes
    private static final int DEFAULT_WAIT_INTERVAL = 5 * 1000; //5 seconds

    public static boolean createTableIfNotExists(final DynamoDbClient dynamo, final String tableName) {
        try {
            dynamo.createTable(createTableRequest(tableName));
            return waitUntilTableActive(dynamo, tableName);
        } catch (ResourceInUseException e) {
            LOG.info("Reused existing table");
        }
        return true;
    }

    public static CompletableFuture<Boolean> createTableIfNotExistsAsync(final DynamoDbAsyncClient dynamo, String table) {
        return dynamo.createTable(DynamoDBUtils.createTableRequest(table))
                .thenCompose(resp -> DynamoDBUtils.waitUntilTableActiveAsync(dynamo, table))
                .exceptionally(th -> {
                    if (th.getCause() instanceof ResourceInUseException) {
                        LOG.info("Reused existing table");
                        return true;
                    } else {
                        LOG.error("Failed table creation", th);
                        return false;
                    }
                });
    }

    private static boolean waitUntilTableActive(final DynamoDbClient dynamo, final String tableName) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + DEFAULT_WAIT_TIMEOUT;

        while (System.currentTimeMillis() < endTime) {
            try {
                TableDescription table = dynamo.describeTable(DescribeTableRequest.builder().tableName(tableName).build())
                        .table();
                if (table.tableStatus().equals(TableStatus.ACTIVE)) {
                    return true;

                }
            } catch (ResourceNotFoundException e) {
                // Table doesn't exist yet. Keep pooling
            }

            try {
                Thread.sleep(DEFAULT_WAIT_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOG.info(tableName + " table - Retry table created status");
        }
        return false;
    }

    private static CompletableFuture<Boolean> waitUntilTableActiveAsync(final DynamoDbAsyncClient dynamo,
            final String table) {
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + DEFAULT_WAIT_TIMEOUT;

        return retryAsync(() -> dynamo.describeTable(DescribeTableRequest.builder().tableName(table).build()), endTime);
    }

    private static CompletableFuture<Boolean> retryAsync(Supplier<CompletableFuture<DescribeTableResponse>> action,
            final long endTime) {

        return action.get()
                .thenComposeAsync(result -> {
                    if (result.table().tableStatus() == TableStatus.ACTIVE) {
                        return CompletableFuture.completedFuture(true);
                    } else {
                        try {
                            Thread.sleep(DEFAULT_WAIT_INTERVAL);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        LOG.info("Async table - Retry table created status");
                        if (System.currentTimeMillis() < endTime) {
                            return retryAsync(action, endTime);
                        } else {
                            return CompletableFuture.completedFuture(false);
                        }
                    }
                });
    }

    private static CreateTableRequest createTableRequest(String table) {
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

    public static PutItemRequest createPutRequest(String table, String keyValue, String rangeValue, String payLoad) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(KEY_NAME, AttributeValue.builder().s(keyValue).build());
        item.put(RANGE_NAME, AttributeValue.builder().s(rangeValue).build());
        item.put(PAYLOAD_NAME, AttributeValue.builder().s(payLoad).build());

        return PutItemRequest.builder()
                .tableName(table)
                .item(item)
                .build();
    }

    public static GetItemRequest createGetRequest(String table, String keyValue, String rangeValue) {
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
