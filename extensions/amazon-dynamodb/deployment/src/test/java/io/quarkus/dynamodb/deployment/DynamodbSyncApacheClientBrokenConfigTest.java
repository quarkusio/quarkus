package io.quarkus.dynamodb.deployment;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.dynamodb.runtime.RuntimeConfigurationError;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamodbSyncApacheClientBrokenConfigTest {

    @Inject
    DynamoDbClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(RuntimeConfigurationError.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("sync-apache-broken-config.properties", "application.properties"));

    @Test
    public void test() {
        Assertions.fail();
    }
}
