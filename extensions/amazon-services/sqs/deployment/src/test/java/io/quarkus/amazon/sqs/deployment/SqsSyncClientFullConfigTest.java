package io.quarkus.amazon.sqs.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsSyncClientFullConfigTest {

    @Inject
    SqsClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
