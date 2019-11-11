package io.quarkus.dynamodb.deployment;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.dynamodb.runtime.RuntimeConfigurationError;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Disabled("https://github.com/quarkusio/quarkus/issues/5286")
public class DynamodbAsyncClientBrokenProxyConfigTest {

    @Inject
    DynamoDbAsyncClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(RuntimeConfigurationError.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("async-broken-proxy-config.properties", "application.properties"));

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
