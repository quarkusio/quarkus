package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Set;
import java.util.function.Predicate;

import jakarta.enterprise.inject.Instance;
import jakarta.json.bind.Jsonb;

import org.eclipse.microprofile.config.ConfigProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.jsonb.JsonbConfigCustomizer;

/**
 * Test whether the underlying Jackson Object Mapper / JSON-B used to create the "built-in" format mapper
 * were modified and cannot be safely replaced by the Hibernate ORM's default ones.
 */
public interface JsonFormatterCustomizationCheck extends Predicate<ArcContainer> {
    @Override
    boolean test(ArcContainer container);

    static JsonFormatterCustomizationCheck jsonFormatterCustomizationCheckSupplier(boolean required, boolean isJackson) {
        if (!required) {
            return new NotModifiedJsonFormatterCustomizationCheck();
        }

        if (isJackson) {
            return new JacksonJsonFormatterCustomizationCheck();
        }
        return new NotModifiedJsonFormatterCustomizationCheck();
    }

    class NotModifiedJsonFormatterCustomizationCheck implements JsonFormatterCustomizationCheck {
        @Override
        public boolean test(ArcContainer container) {
            return false;
        }
    }

    class JacksonJsonFormatterCustomizationCheck implements JsonFormatterCustomizationCheck {
        @Override
        public boolean test(ArcContainer container) {
            InstanceHandle<ObjectMapper> objectMapperInstance = container.instance(ObjectMapper.class);
            if (!objectMapperInstance.isAvailable()) {
                // We have no mapper so it wasn't modified, and we've probably used the JSONB instead.
                return false;
            }
            if (!objectMapperInstance.getBean().isDefaultBean()) {
                // A bean producer was used and the ObjectMapper is a user custom bean...
                return true;
            }

            Instance<ObjectMapperCustomizer> customizers = container.select(ObjectMapperCustomizer.class);
            if (!customizers.isUnsatisfied()) {
                // There most likely are the following customizer available:
                //  - io.quarkus.jackson.customizer.RegisterSerializersAndDeserializersCustomizer  --- this one ... Do we need to check if any serializers were added?
                //  - io.quarkus.jackson.runtime.ConfigurationCustomizer  --- applies the "config properties", we check for those later hence "safe to ignore"
                //  - io.quarkus.jackson.runtime.VertxHybridPoolObjectMapperCustomizer  --- doesn't look like this one affects the serialization -- ignore it
                Set<String> allowedCustomizers = Set.of(
                        "io.quarkus.jackson.customizer.RegisterSerializersAndDeserializersCustomizer",
                        "io.quarkus.jackson.runtime.ConfigurationCustomizer",
                        "io.quarkus.jackson.runtime.VertxHybridPoolObjectMapperCustomizer");
                for (Instance.Handle<ObjectMapperCustomizer> handle : customizers.handles()) {
                    if (allowedCustomizers.contains(handle.getBean().getClass().getName())) {
                        continue;
                    }
                    // ObjectMapper was potentially customized
                    return true;
                }
            }

            // these won't affect representation of the objects in the DB so it's probably somewhat "safe" to allow them:
            Set<String> acceptableConfigs = Set.of("quarkus.jackson.fail-on-unknown-properties",
                    "quarkus.jackson.fail-on-empty-beans");
            for (String propertyName : ConfigProvider.getConfig().getPropertyNames()) {
                if (propertyName.startsWith("quarkus.jackson.") && !acceptableConfigs.contains(propertyName)) {
                    return true;
                }
            }

            return false;
        }
    }

    class JsonbJsonFormatterCustomizationCheck implements JsonFormatterCustomizationCheck {
        @Override
        public boolean test(ArcContainer container) {
            InstanceHandle<Jsonb> jsonbInstance = container.instance(Jsonb.class);
            if (!jsonbInstance.isAvailable()) {
                // We have no JSON-B bean so it wasn't modified.
                return false;
            }
            if (!jsonbInstance.getBean().isDefaultBean()) {
                // A bean producer was used and the JSON-B is a user custom bean...
                return true;
            }

            Instance<JsonbConfigCustomizer> customizers = container.select(JsonbConfigCustomizer.class);
            // There most likely are the following customizer available:
            //  - io.quarkus.jsonb.customizer.RegisterSerializersAndDeserializersCustomizer  --- this one ... Do we need to check if any serializers were added?
            Set<String> allowedCustomizers = Set.of(
                    "io.quarkus.jsonb.customizer.RegisterSerializersAndDeserializersCustomizer");
            for (Instance.Handle<JsonbConfigCustomizer> handle : customizers.handles()) {
                if (allowedCustomizers.contains(handle.getBean().getClass().getName())) {
                    continue;
                }

                // JSON-B was potentially customized
                return true;
            }

            // JSON-B does not have the config properties so nothing else to check..

            return false;
        }
    }
}
