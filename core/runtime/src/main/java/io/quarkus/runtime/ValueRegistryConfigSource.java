package io.quarkus.runtime;

import java.util.Set;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeInfo;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.smallrye.config.common.AbstractConfigSource;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigSource} to bridge between the Config System and
 * {@link ValueRegistry}. This allows the use of {@link io.smallrye.config.SmallRyeConfig} to look up values from
 * {@link ValueRegistry}.
 * <p>
 * While {@link ValueRegistry} shouldn't be exposed in the Config System, this is intended to work as a temporary
 * compatibility layer, since until the introduction of {@link ValueRegistry}, the norm was to use
 * {@link io.smallrye.config.SmallRyeConfig} and System Properties to relay this kind of information, which will be
 * moved to {@link ValueRegistry}, so we need this not to break code that is still relying on the Config system.
 * <p>
 * This should be kept until we decide on an alternate solution in the discussion
 * <a href="https://github.com/quarkusio/quarkus/discussions/46915">#46915</a>.
 *
 * @see "io.quarkus.deployment.steps.RuntimeConfigSetupBuildStep#setupRuntimeConfig"
 * @see "io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.GenerateOperation#run()"
 */
@SuppressWarnings("unused")
public class ValueRegistryConfigSource extends AbstractConfigSource {
    private final ValueRegistry valueRegistry;

    ValueRegistryConfigSource(final ValueRegistry valueRegistry) {
        // ordinal just a bit lower than Build Time Runtime fixed source
        super(ValueRegistryConfigSource.class.getSimpleName(), Integer.MAX_VALUE - 10);
        this.valueRegistry = valueRegistry;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        RuntimeInfo<?> value = valueRegistry.get(propertyName);
        // TODO - We may be required to convert this to the expected config string in Config
        return value != null ? value.get(valueRegistry).toString() : null;
    }

    public static SmallRyeConfigBuilderCustomizer customizer(final ValueRegistry valueRegistry) {
        return new SmallRyeConfigBuilderCustomizer() {
            @Override
            public void configBuilder(SmallRyeConfigBuilder builder) {
                builder.withSources(new ValueRegistryConfigSource(valueRegistry));
            }
        };
    }
}
