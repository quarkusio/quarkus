package io.quarkus.resteasy.reactive.jackson.runtime;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.GeneratedSerializersRegister;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

@Recorder
public class ResteasyReactiveServerJacksonRecorder {

    private static final Map<String, Class<?>> jsonViewMap = new HashMap<>();
    private static final Map<String, Class<?>> customSerializationMap = new HashMap<>();
    private static final Map<String, Class<?>> customDeserializationMap = new HashMap<>();

    public void recordJsonView(String targetId, String className) {
        jsonViewMap.put(targetId, loadClass(className));
    }

    public void recordCustomSerialization(String target, String className) {
        customSerializationMap.put(target, loadClass(className));
    }

    public void recordCustomDeserialization(String target, String className) {
        customDeserializationMap.put(target, loadClass(className));
    }

    public void recordGeneratedSerializer(String className) {
        GeneratedSerializersRegister.addSerializer((Class<? extends StdSerializer>) loadClass(className));
    }

    public void recordGeneratedDeserializer(String className) {
        GeneratedSerializersRegister.addDeserializer((Class<? extends StdDeserializer>) loadClass(className));
    }

    public void configureShutdown(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                jsonViewMap.clear();
                customSerializationMap.clear();
                customDeserializationMap.clear();
            }
        });
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

    private Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for supporting custom JSON serialization", e);
        }
    }
}
