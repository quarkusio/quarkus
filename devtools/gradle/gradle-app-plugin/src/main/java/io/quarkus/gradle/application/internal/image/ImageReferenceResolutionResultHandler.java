package io.quarkus.gradle.application.internal.image;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ImageReferenceResolutionResultHandler implements BiConsumer<Object, Object> {

    private static final String CONTAINER_IMAGE_INFO = "io.quarkus.container.spi.ContainerImageInfoBuildItem";

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object context, Object buildResult) {
        Consumer<ImageReferenceResolution> consumer = (Consumer<ImageReferenceResolution>) context;
        Object imageInfo = invoke(buildResult, "consume", new Class<?>[] { Class.class },
                deploymentClass(buildResult, CONTAINER_IMAGE_INFO));
        consumer.accept(new ImageReferenceResolution(
                (String) invoke(imageInfo, "getImage"),
                (List<String>) invoke(imageInfo, "getAdditionalImageTags")));
    }

    private static Class<?> deploymentClass(Object buildResult, String className) {
        try {
            return Class.forName(className, false, buildResult.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load Quarkus build item class " + className, e);
        }
    }

    private static Object invoke(Object target, String methodName, Object... arguments) {
        Class<?>[] parameterTypes = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            parameterTypes[i] = arguments[i].getClass();
        }
        return invoke(target, methodName, parameterTypes, arguments);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, arguments);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to invoke " + target.getClass().getName() + "." + methodName, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException("Failed to invoke " + target.getClass().getName() + "." + methodName, cause);
        }
    }
}
