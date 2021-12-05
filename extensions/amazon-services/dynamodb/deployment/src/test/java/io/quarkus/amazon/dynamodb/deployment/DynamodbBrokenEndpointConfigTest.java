package io.quarkus.amazon.dynamodb.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.common.runtime.RuntimeConfigurationError;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamodbBrokenEndpointConfigTest {

    @Inject
    DynamoDbClient sync;

    @Inject
    DynamoDbAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(RuntimeConfigurationError.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource("broken-endpoint-config.properties", "application.properties"));

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
