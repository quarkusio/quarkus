package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to pass info about a JAX-RS resource method that needs to perform custom serialization
 */
public final class ResourceMethodCustomSerializationBuildItem extends MultiBuildItem {

    private final MethodInfo methodInfo;
    private final ClassInfo declaringClassInfo;
    private final Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> customSerializationProvider;

    public ResourceMethodCustomSerializationBuildItem(MethodInfo methodInfo, ClassInfo declaringClassInfo,
            Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> customSerializationProvider) {
        this.methodInfo = methodInfo;
        this.declaringClassInfo = declaringClassInfo;
        this.customSerializationProvider = customSerializationProvider;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public ClassInfo getDeclaringClassInfo() {
        return declaringClassInfo;
    }

    public Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> getCustomSerializationProvider() {
        return customSerializationProvider;
    }
}
