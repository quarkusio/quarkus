package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;

class AutoConfigurationCustomizersTest {

    private static final ConfigProperties EMPTY = DefaultConfigProperties.createFromMap(Map.of());

    @Test
    void providersRunInOrderAndRegistrationsCompose() {
        // the second-ordered provider sees (and overwrites) the first one's resource attribute
        AutoConfigurationCustomizers customizers = AutoConfigurationCustomizers.load(loaderOf(
                provider(2, c -> c.addResourceCustomizer(
                        (resource, config) -> resource.toBuilder()
                                .put("order", "second")
                                .put("second", "yes")
                                .build())),
                provider(1, c -> c.addResourceCustomizer(
                        (resource, config) -> resource.toBuilder().put("order", "first").build()))));

        Resource resource = customizers.customizeResource(Resource.empty(), EMPTY);

        assertThat(resource.getAttribute(AttributeKey.stringKey("order"))).isEqualTo("second");
        assertThat(resource.getAttribute(AttributeKey.stringKey("second"))).isEqualTo("yes");
    }

    @Test
    void propertiesSuppliersAreDefaultsAndCustomizersOverride() {
        List<String> invoked = new ArrayList<>();
        AutoConfigurationCustomizers customizers = AutoConfigurationCustomizers.load(loaderOf(
                provider(1, c -> {
                    c.addPropertiesSupplier(() -> Map.of(
                            "otel.supplied", "from-supplier",
                            "otel.existing", "supplier-loses"));
                    c.addPropertiesCustomizer(config -> {
                        invoked.add("empty");
                        return Map.of();
                    });
                    c.addPropertiesCustomizer(config -> {
                        invoked.add("override");
                        return Map.of("otel.existing", "customizer-wins");
                    });
                })));

        ConfigProperties properties = customizers.resolveConfigProperties(Map.of("otel.existing", "original"), loaderOf());

        assertThat(properties.getString("otel.supplied")).isEqualTo("from-supplier");
        assertThat(properties.getString("otel.existing")).isEqualTo("customizer-wins");
        // a customizer returning no overrides is still invoked, as the MicroProfile Telemetry TCK requires
        assertThat(invoked).containsExactly("empty", "override");
    }

    @Test
    void systemPropertiesOverrideSuppliedMapAndCustomizersOverrideBoth() {
        // The legacy AutoConfiguredOpenTelemetrySdk layering: bare otel.* system properties (and OTEL_*
        // environment variables) override the merged map, and properties-customizer overrides beat everything.
        System.setProperty("otel.quarkus.test.layering", "from-system-property");
        System.setProperty("otel.quarkus.test.customized", "from-system-property");
        try {
            AutoConfigurationCustomizers customizers = AutoConfigurationCustomizers.load(loaderOf(
                    provider(1, c -> c.addPropertiesCustomizer(
                            config -> Map.of("otel.quarkus.test.customized", "customizer-wins")))));

            ConfigProperties properties = customizers.resolveConfigProperties(Map.of(
                    "otel.quarkus.test.layering", "from-map",
                    "otel.quarkus.test.customized", "from-map"), loaderOf());

            assertThat(properties.getString("otel.quarkus.test.layering")).isEqualTo("from-system-property");
            assertThat(properties.getString("otel.quarkus.test.customized")).isEqualTo("customizer-wins");
        } finally {
            System.clearProperty("otel.quarkus.test.layering");
            System.clearProperty("otel.quarkus.test.customized");
        }
    }

    @Test
    void componentCustomizersWrapTheGivenInstance() {
        Sampler custom = Sampler.alwaysOff();
        AutoConfigurationCustomizers customizers = AutoConfigurationCustomizers.load(loaderOf(
                provider(1, c -> c.addSamplerCustomizer((sampler, config) -> custom))));

        assertThat(customizers.customizeSampler(Sampler.alwaysOn(), EMPTY)).isSameAs(custom);
    }

    @Test
    void noneIsIdentityEverywhere() {
        AutoConfigurationCustomizers none = AutoConfigurationCustomizers.none();
        Sampler sampler = Sampler.alwaysOn();
        Resource resource = Resource.create(Attributes.of(AttributeKey.stringKey("key"), "value"));

        assertThat(none.customizeSampler(sampler, EMPTY)).isSameAs(sampler);
        assertThat(none.customizeResource(resource, EMPTY)).isSameAs(resource);
        assertThat(none.resolveConfigProperties(Map.of("otel.key", "value"), loaderOf()).getString("otel.key"))
                .isEqualTo("value");
    }

    private static AutoConfigurationCustomizerProvider provider(int order,
            java.util.function.Consumer<AutoConfigurationCustomizer> registration) {
        return new AutoConfigurationCustomizerProvider() {
            @Override
            public void customize(AutoConfigurationCustomizer autoConfiguration) {
                registration.accept(autoConfiguration);
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    private static ComponentLoader loaderOf(AutoConfigurationCustomizerProvider... providers) {
        return new ComponentLoader() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Iterable<T> load(Class<T> type) {
                return type == AutoConfigurationCustomizerProvider.class
                        ? (Iterable<T>) List.of(providers)
                        : List.of();
            }
        };
    }
}
