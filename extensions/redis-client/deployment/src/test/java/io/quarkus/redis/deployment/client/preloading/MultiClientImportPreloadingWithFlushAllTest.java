package io.quarkus.redis.deployment.client.preloading;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class MultiClientImportPreloadingWithFlushAllTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.redis.hosts=${quarkus.redis.tr}\n"
                            + "quarkus.redis.load-script=import/my-import.redis\n"
                            + "quarkus.redis.my-redis.hosts=${quarkus.redis.tr}\n"
                            + "quarkus.redis.my-redis.load-script=sample.redis\n" +
                            // Erase the data source
                            // Bad idea (as it will dropped what we loaded so far), but just to test the behavior
                            "quarkus.redis.my-redis.flush-before-load=true"), "application.properties")
                    .addAsResource(new File("src/test/resources/imports/import.redis"), "import/my-import.redis")
                    .addAsResource(new File("src/test/resources/imports/sample.redis"), "sample.redis")

            );

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("my-redis")
    RedisDataSource my;

    @Test
    void verifyImport() {
        // Others have been removed by the `flushall` command
        assertThat(my.key().keys("*")).containsOnly("key", "space:key", "counter");
    }
}
