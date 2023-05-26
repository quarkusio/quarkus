package io.quarkus.redis.deployment.client.preloading;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class MissingFilePreloadTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "quarkus.redis.hosts=${quarkus.redis.tr}\n" +
                                    "quarkus.redis.load-script=import/my-import.redis,missing.redis"),
                            "application.properties")
                    .addAsResource(new File("src/test/resources/imports/import.redis"), "import/my-import.redis"))
            .assertException(t -> assertThat(t).hasMessageContaining("Unable to find file referenced"));

    @Inject
    RedisDataSource ds;

    @Test
    void test() {
        // should not run
    }
}
