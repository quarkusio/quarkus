package io.quarkus.dynamodb.deployment;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Disabled("Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the async"
        + " support.")
public class DynamodbAsyncClientFullConfigTest {

    @Inject
    DynamoDbAsyncClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("async-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // Application should start with full config.
    }
}
