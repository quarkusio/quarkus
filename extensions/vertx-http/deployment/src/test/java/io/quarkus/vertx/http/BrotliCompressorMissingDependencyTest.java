package io.quarkus.vertx.http;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Brotli4J is an optional dependency of quarkus-vertx-http, so enabling the "br" compressor without adding it must
 * fail the build with an actionable message instead of failing at runtime.
 */
public class BrotliCompressorMissingDependencyTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.http.compressors=gzip,deflate,br\n"),
                            "application.properties"))
            .assertException(t -> {
                Assertions.assertThat(t).isInstanceOf(ConfigurationException.class);
                Assertions.assertThat(t.getMessage())
                        .contains("com.aayushatharva.brotli4j:brotli4j")
                        .contains("quarkus.http.compressors");
            });

    @Test
    public void test() {
        Assertions.fail("The build should have failed because Brotli4J is not on the classpath");
    }
}
