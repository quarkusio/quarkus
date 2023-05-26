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

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class NonDefaultFileForDefaultClientImportPreloadingTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "quarkus.redis.hosts=${quarkus.redis.tr}\n" +
                                    "quarkus.redis.load-script=import/my-import.redis"),
                            "application.properties")
                    .addAsResource(new File("src/test/resources/imports/import.redis"), "import/my-import.redis"));

    @Inject
    RedisDataSource ds;

    @Test
    void verifyImport() {
        var keys = ds.key();
        var values = ds.value(String.class);
        var hashes = ds.hash(String.class);

        assertThat(keys.keys("*")).containsExactlyInAnyOrder("foo", "bar", "key1", "key2", "key3", "key4");

        assertThat(hashes.hgetall("foo")).containsOnly(entry("field1", "abc"), entry("field2", "123"));
        assertThat(hashes.hgetall("bar")).containsOnly(entry("field1", "abc def"), entry("field2", "123 456 "));

        assertThat(values.get("key1")).isEqualTo("A value using \"double-quotes\"");
        assertThat(values.get("key2")).isEqualTo("A value using 'single-quotes'");
        assertThat(values.get("key3")).isEqualTo("A value using a single single ' quote");
        assertThat(values.get("key4")).isEqualTo("A value using a single double \" quote");
    }
}
