package io.quarkus.test.junit;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class NativeDevServicesHandler implements BiConsumer<Object, Object> {

    private static final String DEV_SERVICES_ADDITIONAL_CONFIG = "io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem";
    private static final String DEV_SERVICES_CUSTOMIZER = "io.quarkus.deployment.builditem.DevServicesCustomizerBuildItem";
    private static final String DEV_SERVICES_LAUNCHER_CONFIG = "io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem";
    private static final String DEV_SERVICES_NETWORK_ID = "io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem";
    private static final String DEV_SERVICES_REGISTRY = "io.quarkus.deployment.builditem.DevServicesRegistryBuildItem";
    private static final String DEV_SERVICES_RESULT = "io.quarkus.deployment.builditem.DevServicesResultBuildItem";

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object o, Object buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;

        Object compose = consumeOptional(buildResult, DEV_SERVICES_NETWORK_ID);
        Object devServicesProperties = consume(buildResult, DEV_SERVICES_LAUNCHER_CONFIG);
        for (var entry : ((Map<String, String>) invoke(devServicesProperties, "getConfig")).entrySet()) {
            propertyConsumer.accept(entry.getKey(), entry.getValue());
        }
        if (compose != null && invoke(compose, "getNetworkId") != null) {
            propertyConsumer.accept("quarkus.test.container.network", (String) invoke(compose, "getNetworkId"));
        }

        List<?> devServices = consumeMulti(buildResult, DEV_SERVICES_RESULT);
        Object devServicesRegistry = consumeOptional(buildResult, DEV_SERVICES_REGISTRY);
        List<?> customizers = consumeMulti(buildResult, DEV_SERVICES_CUSTOMIZER);
        List<?> additionalConfigBuildItems = consumeMulti(buildResult, DEV_SERVICES_ADDITIONAL_CONFIG);
        if (devServicesRegistry != null) {
            Object startResult = invoke(devServicesRegistry, "startAll",
                    new Class<?>[] { Collection.class, List.class, List.class, ClassLoader.class },
                    devServices, customizers, additionalConfigBuildItems, null);
            for (Map.Entry<String, String> entry : ((Map<String, String>) invoke(startResult, "configs")).entrySet()) {
                propertyConsumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    private static Object consume(Object buildResult, String className) {
        return invoke(buildResult, "consume", new Class<?>[] { Class.class }, deploymentClass(buildResult, className));
    }

    private static Object consumeOptional(Object buildResult, String className) {
        return invoke(buildResult, "consumeOptional", new Class<?>[] { Class.class },
                deploymentClass(buildResult, className));
    }

    private static List<?> consumeMulti(Object buildResult, String className) {
        return (List<?>) invoke(buildResult, "consumeMulti", new Class<?>[] { Class.class },
                deploymentClass(buildResult, className));
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
