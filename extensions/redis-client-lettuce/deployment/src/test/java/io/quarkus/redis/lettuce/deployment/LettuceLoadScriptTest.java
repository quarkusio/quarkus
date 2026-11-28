package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that the load script of a client is still imported with the Lettuce backend, for a client used only
 * through its data source: the import runs through the Vert.x Redis client, which is created for it although no
 * Vert.x Redis client bean is.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceLoadScriptTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(VertxClientBeans.class)
                    .addAsResource(new StringAsset("SET lettuce-load-script imported\n"), "import.redis"))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.flush-before-load", "true");

    @Inject
    RedisDataSource ds;

    @Test
    void theLoadScriptIsImported() {
        assertThat(ds.value(String.class).get("lettuce-load-script")).isEqualTo("imported");
        assertThat(VertxClientBeans.clientNames()).isEmpty();
    }
}
