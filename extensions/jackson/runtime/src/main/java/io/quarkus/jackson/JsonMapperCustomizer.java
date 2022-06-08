package io.quarkus.jackson;

import com.fasterxml.jackson.databind.json.JsonMapper;

import io.quarkus.jackson.runtime.JsonMapperProducer;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for the default {@link JsonMapper}.
 * <p>
 * All implementations (that are registered as CDI beans) are taken into account when producing the default
 * {@link JsonMapper}.
 * <p>
 * See also {@link JsonMapperProducer#jsonMapper}.
 */
public interface JsonMapperCustomizer extends ObjectMapperCustomizer {

    int MINIMUM_PRIORITY = Integer.MIN_VALUE;
    // we use this priority to give a chance to other customizers to override serializers / deserializers
    // that might have been added by the modules that Quarkus registers automatically
    // (Jackson will keep the last registered serializer / deserializer for a given type
    // if multiple are registered)
    int QUARKUS_CUSTOMIZER_PRIORITY = MINIMUM_PRIORITY + 100;
    int DEFAULT_PRIORITY = 0;

    void customize(JsonMapper.Builder jsonMapperBuilder);

    /**
     * Defines the priority that the customizers are applied.
     * A lower integer value means that the customizer will be applied after a customizer with a higher priority
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    default int compareTo(JsonMapperCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
