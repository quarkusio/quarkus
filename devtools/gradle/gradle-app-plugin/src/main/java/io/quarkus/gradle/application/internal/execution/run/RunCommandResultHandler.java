package io.quarkus.gradle.application.internal.execution.run;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RunCommandResultHandler implements BiConsumer<Object, Object> {

    private static final String RUN_COMMAND_RESULT = "io.quarkus.deployment.cmd.RunCommandActionResultBuildItem";
    private static final String DEV_SERVICES_LAUNCHER_CONFIG = "io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem";
    private static final String DEV_SERVICES_REGISTRY = "io.quarkus.deployment.builditem.DevServicesRegistryBuildItem";
    private static final String DEV_SERVICES_RESULT = "io.quarkus.deployment.builditem.DevServicesResultBuildItem";
    private static final String DEV_SERVICES_CUSTOMIZER = "io.quarkus.deployment.builditem.DevServicesCustomizerBuildItem";
    private static final String DEV_SERVICES_ADDITIONAL_CONFIG = "io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem";

    private static final Runnable NO_DEV_SERVICES_TO_CLOSE = () -> {
    };

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object context, Object buildResult) {
        Consumer<RunCommandResult> consumer = (Consumer<RunCommandResult>) context;
        Object runCommands = consume(buildResult, RUN_COMMAND_RESULT);
        var devServices = devServicesConfig(buildResult);

        Map<String, List<?>> commands = new HashMap<>();
        for (Object command : (List<?>) invoke(runCommands, "getCommands")) {
            commands.put((String) invoke(command, "getCommandName"), command(command, devServices.config()));
        }
        consumer.accept(new RunCommandResult(commands, devServices.close()));
    }

    @SuppressWarnings("unchecked")
    private static DevServicesRunResult devServicesConfig(Object buildResult) {
        Map<String, String> config = new HashMap<>();
        Object launcherConfig = consumeOptional(buildResult, DEV_SERVICES_LAUNCHER_CONFIG);
        if (launcherConfig != null) {
            config.putAll((Map<String, String>) invoke(launcherConfig, "getConfig"));
        }

        Object registry = consumeOptional(buildResult, DEV_SERVICES_REGISTRY);
        Runnable close = NO_DEV_SERVICES_TO_CLOSE;
        if (registry != null) {
            Object started = invoke(registry, "startAll",
                    new Class<?>[] { java.util.Collection.class, List.class, List.class, ClassLoader.class },
                    consumeMulti(buildResult, DEV_SERVICES_RESULT),
                    consumeMulti(buildResult, DEV_SERVICES_CUSTOMIZER),
                    consumeMulti(buildResult, DEV_SERVICES_ADDITIONAL_CONFIG),
                    Thread.currentThread().getContextClassLoader());
            config.putAll((Map<String, String>) invoke(started, "configs"));
            close = () -> invoke(registry, "closeOwnRunningServices");
        }
        return new DevServicesRunResult(config, close);
    }

    @SuppressWarnings("unchecked")
    private static List<?> command(Object command, Map<String, String> devServicesConfig) {
        List<Object> values = new ArrayList<>();
        values.add(arguments((List<String>) invoke(command, "getArgs"), devServicesConfig));
        values.add(invoke(command, "getWorkingDirectory"));
        values.add(invoke(command, "getStartedExpression"));
        values.add(invoke(command, "isNeedsLogfile"));
        values.add(invoke(command, "getLogFile"));
        return values;
    }

    private static List<String> arguments(List<String> original, Map<String, String> devServicesConfig) {
        if (devServicesConfig.isEmpty()) {
            return original;
        }
        int jarIndex = original.indexOf("-jar");
        if (jarIndex < 0) {
            return original;
        }
        List<String> effective = new ArrayList<>(original.size() + devServicesConfig.size());
        effective.addAll(original.subList(0, jarIndex));
        devServicesConfig.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> effective.add("-D" + entry.getKey() + "=" + entry.getValue()));
        effective.addAll(original.subList(jarIndex, original.size()));
        return effective;
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

    private record DevServicesRunResult(Map<String, String> config, Runnable close) {
    }
}
