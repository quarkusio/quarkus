package io.quarkus.amazon.dynamodb.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamodbClientDefaultCredentialsProviderConfigTest {

    @Inject
    DynamoDbAsyncClient async;

    @Inject
    DynamoDbClient sync;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("default-credentials-config.properties", "application.properties"));

    @Test
    public void test() {
        // Application should start with full config.
    }
}
