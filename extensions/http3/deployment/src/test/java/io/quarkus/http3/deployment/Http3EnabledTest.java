package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http3.runtime.Http3Customizer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.HttpServerConfigCustomizer;

public class Http3EnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    Instance<HttpServerConfigCustomizer> customizers;

    @Test
    public void testHttp3CustomizerPresent() {
        assertThat(customizers.stream())
                .anyMatch(c -> c instanceof Http3Customizer);
    }

}
