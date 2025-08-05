package io.quarkus.jaxrs.client.reactive.deployment;

import java.util.Map;

import io.quarkus.gizmo.MethodDescriptor;

public class MethodDescriptors {
    public static final MethodDescriptor THREAD_CURRENT_THREAD = MethodDescriptor.ofMethod(Thread.class, "currentThread",
            Thread.class);

    public static final MethodDescriptor THREAD_GET_TCCL = MethodDescriptor.ofMethod(Thread.class, "getContextClassLoader",
            ClassLoader.class);

    public static final MethodDescriptor CL_FOR_NAME = MethodDescriptor.ofMethod(Class.class, "forName",
            Class.class, String.class, boolean.class, ClassLoader.class);

    public static final MethodDescriptor MAP_PUT = MethodDescriptor.ofMethod(Map.class, "put",
            Object.class, Object.class, Object.class);
}
