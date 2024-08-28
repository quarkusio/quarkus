package io.quarkus.redis.deployment.client.preloading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
public class MultiClientImportPreloadingTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "quarkus.redis.hosts=${quarkus.redis.tr}\n" +
                                    "quarkus.redis.load-script=import/my-import.redis\n" +
                                    "quarkus.redis.my-redis.hosts=${quarkus.redis.tr}\n" +
                                    "quarkus.redis.my-redis.load-script=sample.redis\n" +
                                    // Do not erase as it's using the same database
                                    // And load even if not empty
                                    "quarkus.redis.my-redis.flush-before-load=false\n" +
                                    "quarkus.redis.my-redis.load-only-if-empty=false"),
                            "application.properties")
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
        // Both clients are using the same database, by using multiple clients to distinguish.
        var values = ds.value(String.class);
        var hashes = ds.hash(String.class);

        assertThat(hashes.hgetall("foo")).containsOnly(entry("field1", "abc"), entry("field2", "123"));
        assertThat(hashes.hgetall("bar")).containsOnly(entry("field1", "abc def"), entry("field2", "123 456 "));

        assertThat(values.get("key1")).isEqualTo("A value using \"double-quotes\"");
        assertThat(values.get("key2")).isEqualTo("A value using 'single-quotes'");
        assertThat(values.get("key3")).isEqualTo("A value using a single single ' quote");
        assertThat(values.get("key4")).isEqualTo("A value using a single double \" quote");

        values = my.value(String.class);
        assertThat(values.get("key")).isEqualTo("value");
        assertThat(values.get("space:key")).isEqualTo("another value");
        assertThat(values.get("counter")).isEqualTo("1");
    }
}
