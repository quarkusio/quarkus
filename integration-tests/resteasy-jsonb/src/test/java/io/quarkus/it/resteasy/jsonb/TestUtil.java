package io.quarkus.it.resteasy.jsonb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;
import javax.json.bind.serializer.JsonbSerializer;

import org.eclipse.yasson.internal.JsonbContext;

import io.quarkus.resteasy.jsonb.runtime.serializers.QuarkusJsonbBinding;

final class TestUtil {

    static Jsonb getConfiguredJsonb() {
        try {
            Class<?> jsonbResolverClass = Class.forName("io.quarkus.jsonb.QuarkusJsonbContextResolver");
            Object jsonbResolverObject = jsonbResolverClass.newInstance();
            Method getContext = jsonbResolverClass.getMethod("getContext", Class.class);
            return (Jsonb) getContext.invoke(jsonbResolverObject, Jsonb.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static JsonbConfig getConfiguredJsonbConfig() {
        try {
            QuarkusJsonbBinding configuredJsonb = (QuarkusJsonbBinding) getConfiguredJsonb();
            Field jsonbContextField = configuredJsonb.getClass().getDeclaredField("jsonbContext");
            jsonbContextField.setAccessible(true);
            JsonbContext jsonbContext = (JsonbContext) jsonbContextField.get(configuredJsonb);
            return jsonbContext.getConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static List<JsonbSerializer> getConfiguredJsonbSerializers() {
        JsonbConfig jsonbConfig = getConfiguredJsonbConfig();
        Optional<Object> property = jsonbConfig.getProperty(JsonbConfig.SERIALIZERS);
        return property.map(o -> Arrays.asList((JsonbSerializer[]) o)).orElse(Collections.emptyList());
    }
}
