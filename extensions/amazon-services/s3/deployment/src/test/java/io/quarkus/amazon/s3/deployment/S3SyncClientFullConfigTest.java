package io.quarkus.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.s3.S3Client;

public class S3SyncClientFullConfigTest {

    @Inject
    Instance<S3Client> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        assertNotNull(client.get());
        // should finish with success
    }
}
