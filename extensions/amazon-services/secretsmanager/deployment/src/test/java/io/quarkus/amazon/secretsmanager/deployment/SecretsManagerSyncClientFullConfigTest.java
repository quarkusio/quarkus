package io.quarkus.amazon.secretsmanager.deployment;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.secretsmanager.runtime.AWSSecretsManager;
import io.quarkus.amazon.secretsmanager.runtime.AWSSecretsManagerReader;
import io.quarkus.test.Mock;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.secretsmanager.*;

public class SecretsManagerSyncClientFullConfigTest {

    @Inject
    SecretsManagerClient client;

    @Inject
    SecretsManagerAsyncClient asyncClient;

    final static String SECRET_ID = "someSecretId";
    @AWSSecretsManager(SECRET_ID)
    @Inject
    String secretId;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        Assertions.assertEquals(SECRET_ID, secretId);
    }

    @Mock
    public static class MockAWSSecretsManagerReader extends AWSSecretsManagerReader {
        public String getSecret(final String key) {
            return key;
        }
    }
}
