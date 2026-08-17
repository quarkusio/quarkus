package io.quarkus.test.junit;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

public class NativeDevServicesHandler implements BiConsumer<Object, Object> {

    private static final Logger log = Logger.getLogger(NativeDevServicesHandler.class);

    private static final String DEV_SERVICES_ADDITIONAL_CONFIG = "io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem";
    private static final String DEV_SERVICES_CUSTOMIZER = "io.quarkus.deployment.builditem.DevServicesCustomizerBuildItem";
    private static final String DEV_SERVICES_LAUNCHER_CONFIG = "io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem";
    private static final String DEV_SERVICES_NETWORK_ID = "io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem";
    private static final String DEV_SERVICES_REGISTRY = "io.quarkus.deployment.builditem.DevServicesRegistryBuildItem";
    private static final String DEV_SERVICES_RESULT = "io.quarkus.deployment.builditem.DevServicesResultBuildItem";
    private static final String DEV_SERVICES_SHARED_NETWORK = "io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem";

    /**
     * The source {@code IntegrationTestUtil} identifies itself with when it asks for a shared network because the
     * application is going to be launched as a container.
     */
    private static final String INTEGRATION_TEST_SOURCE = "io.quarkus.test.junit";

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
        if (compose == null && !devServices.isEmpty() && isLaunchedAsContainer(buildResult)) {
            // Without this the only symptom is the application container failing to resolve the Dev Service hostname,
            // which surfaces 60 seconds later as an unrelated-looking "Unable to determine the status of the running
            // process". This is a warning rather than a failure because we cannot tell here whether the Dev Services
            // about to start will actually create containers.
            log.warnf("Dev Services are in use and the application is launched as a container, but no Dev Services"
                    + " network id was reported, so the application container may not be able to reach the Dev Services"
                    + " containers. The build step reporting that network id ships in"
                    + " io.quarkus:quarkus-devservices-common; add that dependency to the deployment module of the"
                    + " extension providing the Dev Service, and join the network through"
                    + " io.quarkus.devservices.common.ConfigureUtil.configureSharedNetwork().");
        }
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

    /**
     * Whether the shared network was requested because the application itself is launched as a container, as opposed to
     * having been asked for by configuration or by an extension.
     */
    private static boolean isLaunchedAsContainer(Object buildResult) {
        for (Object sharedNetwork : consumeMulti(buildResult, DEV_SERVICES_SHARED_NETWORK)) {
            if (INTEGRATION_TEST_SOURCE.equals(invoke(sharedNetwork, "getSource"))) {
                return true;
            }
        }
        return false;
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
