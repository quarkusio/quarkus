package io.quarkus.resteasy.reactive.jackson.runtime;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyReactiveServerJacksonRecorder {

    private static final Map<String, Class<?>> jsonViewMap = new HashMap<>();
    private static final Map<String, Class<?>> customSerializationMap = new HashMap<>();

    public void recordJsonView(String methodId, String className) {
        jsonViewMap.put(methodId, loadClass(className));
    }

    public void recordCustomSerialization(String methodId, String className) {
        customSerializationMap.put(methodId, loadClass(className));
    }

    public void configureShutdown(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                jsonViewMap.clear();
                customSerializationMap.clear();
            }
        });
    }

    public static Class<?> jsonViewForMethod(String methodId) {
        return jsonViewMap.get(methodId);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> customSerializationForMethod(String methodId) {
        return (Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>>) customSerializationMap.get(methodId);
    }

    private Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for supporting custom JSON serialization", e);
        }
    }
}
