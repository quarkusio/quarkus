package io.quarkus.resteasy.reactive.jackson.runtime;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.GeneratedSerializersRegister;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

public final class ResteasyReactiveServerJacksonRecorder {

    private static final Map<String, Class<?>> jsonViewMap = new HashMap<>();
    private static final Map<String, Class<?>> customSerializationMap = new HashMap<>();
    private static final Map<String, Class<?>> customDeserializationMap = new HashMap<>();

    public static void registerJsonAnnotations(Map<String, String> jsonViews, Map<String, String> customSerializations,
            Map<String, String> customDeserializations) {
        jsonViews.forEach((target, className) -> jsonViewMap.put(target, loadClass(className)));
        customSerializations.forEach((target, className) -> customSerializationMap.put(target, loadClass(className)));
        customDeserializations.forEach((target, className) -> customDeserializationMap.put(target, loadClass(className)));
    }

    public static void clearJsonAnnotations() {
        jsonViewMap.clear();
        customSerializationMap.clear();
        customDeserializationMap.clear();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void registerGeneratedSerializers(List<String> serializers, List<String> deserializers) {
        for (String className : serializers) {
            GeneratedSerializersRegister.addSerializer((Class<? extends StdSerializer>) loadClass(className));
        }
        for (String className : deserializers) {
            GeneratedSerializersRegister.addDeserializer((Class<? extends StdDeserializer>) loadClass(className));
        }
    }

    public static Class<?> jsonViewForClass(Class<?> clazz) {
        return jsonViewMap.get(clazz.getName());
    }

    public static Class<?> jsonViewForMethod(String methodId) {
        return jsonViewMap.get(methodId);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> customSerializationForMethod(String methodId) {
        return (Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>>) customSerializationMap.get(methodId);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> customSerializationForClass(Class<?> clazz) {
        return (Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>>) customSerializationMap.get(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>> customDeserializationForMethod(
            String methodId) {
        return (Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>>) customDeserializationMap.get(methodId);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>> customDeserializationForClass(Class<?> clazz) {
        return (Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>>) customDeserializationMap.get(clazz.getName());
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for supporting custom JSON serialization", e);
        }
    }
}
