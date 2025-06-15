package io.quarkus.micrometer.runtime;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for various {@link MeterRegistry} classes
 * registered by Quarkus.
 * <p>
 * Unless an implementation is annotated with {@link MeterRegistryCustomizerConstraint}, it will apply to all
 * {@link MeterRegistry} classes.
 */
public interface MeterRegistryCustomizer extends Comparable<MeterRegistryCustomizer> {

    int MINIMUM_PRIORITY = Integer.MIN_VALUE;
    // we use this priority to give a chance to other customizers to override serializers / deserializers
    // that might have been added by the modules that Quarkus registers automatically
    // (Jackson will keep the last registered serializer / deserializer for a given type
    // if multiple are registered)
    int QUARKUS_CUSTOMIZER_PRIORITY = MINIMUM_PRIORITY + 100;
    int DEFAULT_PRIORITY = 0;

    void customize(MeterRegistry registry);

    default int priority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    default int compareTo(MeterRegistryCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
