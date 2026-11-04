package io.quarkus.opentelemetry.deployment.traces;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.Config;

public class TestModeSamplerDefaultTest {

    // No application.properties: the app uses the default sampler
    // (parentbased_traceidratio) and does NOT set sampler.arg.
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar.addClasses());

    @Test
    void testModeDefaultsSamplerArgTo100Percent() {
        String samplerArg = Config.get()
                .getValue("quarkus.otel.traces.sampler.arg", String.class);
        assertThat(samplerArg).isEqualTo("1.0d");
    }
}
