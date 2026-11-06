package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class BrotliWithoutBrotli4jTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.compressors", "gzip,br")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("com.aayushatharva.brotli4j:brotli4j")
                    .hasMessageContaining("native-<os>-<arch>"));

    @Test
    public void test() {
        fail("The build should have failed");
    }
}
