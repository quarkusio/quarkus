package io.quarkus.jaeger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.NumberFormat;
import java.util.Locale;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.jaegertracing.Configuration.SamplerConfiguration;
import io.quarkus.jaeger.runtime.JaegerConfig;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Some Locales, like FRENCH use a different separator between the integer and the fraction part. The internal
 * Jaeger configuration, read the value with the default locale, so we need the config to write the value to Jaeger in
 * the same expected format.
 */
public class CommaParseJaegerConfigurationTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication()
            .setBeforeAllCustomizer(() -> Locale.setDefault(Locale.FRENCH))
            .setAfterAllCustomizer(() -> Locale.setDefault(DEFAULT_LOCALE))
            .overrideConfigKey("quarkus.jaeger.sampler-type", "probabilistic")
            .overrideConfigKey("quarkus.jaeger.sampler-param", "0.5");

    @Inject
    JaegerConfig jaegerConfig;

    @Test
    void localeParse() {
        assertEquals("0,5", NumberFormat.getInstance().format(jaegerConfig.samplerParam.get()));

        SamplerConfiguration samplerConfiguration = SamplerConfiguration.fromEnv();
        assertEquals(0.5d, samplerConfiguration.getParam().doubleValue());
    }
}
