package io.quarkus.amazon.dynamodb.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.common.runtime.RuntimeConfigurationError;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamodbProcessCredentialsBrokenConfigTest {

    @Inject
    DynamoDbAsyncClient async;

    @Inject
    DynamoDbClient sync;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(RuntimeConfigurationError.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource("process-credentials-broken-config.properties", "application.properties"));

    @Test
    public void test() {
        // should not be called, deployment exception should happen first:
        // it's illegal to use PROCESS credentials resolver without command specified
        Assertions.fail();
    }
}
