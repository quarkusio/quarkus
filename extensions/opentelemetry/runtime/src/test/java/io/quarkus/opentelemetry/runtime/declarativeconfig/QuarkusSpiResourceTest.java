package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

class QuarkusSpiResourceTest {

    @Test
    void spiResourceProvidersAreMergedInOrder() {
        // FirstResourceProvider has the lower order, so SecondResourceProvider's value wins the shared key
        ComponentLoader loader = loader(List.of(new SecondResourceProvider(), new FirstResourceProvider()));

        Resource resource = QuarkusSpiResource.create(loader,
                DefaultConfigProperties.createFromMap(Map.of()));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isEqualTo("first");
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("second");
    }

    @Test
    void disabledSpiResourceProviderIsSkipped() {
        ComponentLoader loader = loader(List.of(new FirstResourceProvider(), new SecondResourceProvider()));

        Resource resource = QuarkusSpiResource.create(loader,
                DefaultConfigProperties.createFromMap(Map.of(
                        "otel.java.disabled.resource.providers", SecondResourceProvider.class.getName())));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isEqualTo("first");
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("first");
    }

    @Test
    void enabledSpiResourceProvidersActAsAllowList() {
        ComponentLoader loader = loader(List.of(new FirstResourceProvider(), new SecondResourceProvider()));

        Resource resource = QuarkusSpiResource.create(loader,
                DefaultConfigProperties.createFromMap(Map.of(
                        "otel.java.enabled.resource.providers", SecondResourceProvider.class.getName())));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isNull();
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("second");
    }

    @Test
    void conditionalSpiResourceProviderIsConsulted() {
        ComponentLoader loader = loader(List.of(new NeverApplyResourceProvider()));

        Resource resource = QuarkusSpiResource.create(loader,
                DefaultConfigProperties.createFromMap(Map.of()));

        assertThat(resource.getAttribute(AttributeKey.stringKey("never.key"))).isNull();
    }

    @Test
    void disabledResourceKeysAreFiltered() {
        ComponentLoader loader = loader(List.of(new FirstResourceProvider()));

        Resource resource = QuarkusSpiResource.create(loader,
                DefaultConfigProperties.createFromMap(Map.of("otel.resource.disabled.keys", "first.key")));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isNull();
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("first");
    }

    public static class FirstResourceProvider implements ResourceProvider {
        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.create(Attributes.of(
                    AttributeKey.stringKey("first.key"), "first",
                    AttributeKey.stringKey("shared.key"), "first"));
        }

        @Override
        public int order() {
            return 1;
        }
    }

    public static class SecondResourceProvider implements ResourceProvider {
        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.create(Attributes.of(AttributeKey.stringKey("shared.key"), "second"));
        }

        @Override
        public int order() {
            return 2;
        }
    }

    public static class NeverApplyResourceProvider implements ConditionalResourceProvider {
        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.create(Attributes.of(AttributeKey.stringKey("never.key"), "never"));
        }

        @Override
        public boolean shouldApply(ConfigProperties config, Resource existing) {
            return false;
        }
    }

    // Loads the resource providers
    private static ComponentLoader loader(List<ResourceProvider> providers) {
        return new ComponentLoader() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Iterable<T> load(Class<T> type) {
                return type == ResourceProvider.class ? (Iterable<T>) providers : List.of();
            }
        };
    }
}
