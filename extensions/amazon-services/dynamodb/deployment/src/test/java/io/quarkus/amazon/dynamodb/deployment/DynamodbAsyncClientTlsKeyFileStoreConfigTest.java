package io.quarkus.amazon.dynamodb.deployment;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class DynamodbAsyncClientTlsKeyFileStoreConfigTest {

    @Inject
    DynamoDbAsyncClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("async-tls-key-filestore-config.properties", "application.properties"));

    @Test
    public void test() {
        // Application should start with full config.
    }
}
