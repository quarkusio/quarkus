package io.quarkus.dev.console;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.dev.spi.HotReplacementContext;

public class DevConsoleManager {
    public static volatile String DEV_MANAGER_GLOBALS_ASSISTANT = "_assistant";
    private static volatile Consumer<DevConsoleRequest> handler;
    private static volatile Map<String, Map<String, Object>> templateInfo;
    private static volatile HotReplacementContext hotReplacementContext;
    private static volatile Object quarkusBootstrap;
    private static volatile boolean doingHttpInitiatedReload;
    /**
     * Global map that can be used to share data between the runtime and deployment side
     * to enable communication.
     * <p>
     * Key names should be namespaced.
     * <p>
     * As the class loaders are different these objects will generally need to implement some kind of common interface
     */
    private static final Map<String, Object> globals = new ConcurrentHashMap<>();

    public static void registerHandler(Consumer<DevConsoleRequest> requestHandler) {
        handler = requestHandler;
    }

    public static void sentRequest(DevConsoleRequest request) {
        Consumer<DevConsoleRequest> h = DevConsoleManager.handler;
        if (h == null) {
            request.getResponse().complete(new DevConsoleResponse(503, Collections.emptyMap(), new byte[0])); //service unavailable
        } else {
            h.accept(request);
        }
    }

    public static Map<String, Map<String, Object>> getTemplateInfo() {
        return templateInfo;
    }

    public static void setTemplateInfo(Map<String, Map<String, Object>> templateInfo) {
        DevConsoleManager.templateInfo = templateInfo;
    }

    public static HotReplacementContext getHotReplacementContext() {
        return hotReplacementContext;
    }

    public static void setHotReplacementContext(HotReplacementContext hotReplacementContext) {
        DevConsoleManager.hotReplacementContext = hotReplacementContext;
    }

    public static void setQuarkusBootstrap(Object qb) {
        quarkusBootstrap = qb;
    }

    public static Object getQuarkusBootstrap() {
        return quarkusBootstrap;
    }

    /**
     * Sets a global that is shared between both the runtime and deployment parts
     *
     * @param name A namespaced key
     * @param value A value
     */
    public static void setGlobal(String name, Object value) {
        globals.put(name, value);
    }

    /**
     * Gets a shared global
     *
     * @param name The key
     * @return The value
     */
    public static <T> T getGlobal(String name) {
        return (T) globals.get(name);
    }

    public static boolean isDoingHttpInitiatedReload() {
        return doingHttpInitiatedReload;
    }

    public static void setDoingHttpInitiatedReload(boolean doingHttpInitiatedReload) {
        DevConsoleManager.doingHttpInitiatedReload = doingHttpInitiatedReload;
    }

    public static void close() {
        handler = null;
        templateInfo = null;
        hotReplacementContext = null;
        quarkusBootstrap = null;
        actions.clear();
        assistantActions.clear();
        globals.clear();
    }

    /**
     * A list of action that can be executed.
     * The action registered here should be used with the Dev UI / JSON RPC services.
     */
    private static final Map<String, Function<Map<String, String>, ?>> actions = new HashMap<>();
    private static final Map<String, BiFunction<Object, Map<String, String>, ?>> assistantActions = new HashMap<>();

    /**
     * Registers an action that will be called by a JSON RPC service at runtime
     *
     * @param name the name of the action, should be namespaced to avoid conflicts
     * @param action the action. The function receives a Map as parameters (named parameters) and returns an object of type
     *        {@code T}.
     *        Note that the type {@code T} must be a class shared by both the deployment and the runtime.
     */
    public static <T> void register(String name, Function<Map<String, String>, T> action) {
        actions.put(name, action);
    }

    /**
     * Registers an action that will be called by a JSON RPC service at runtime, this action will include the assistant if
     * available
     *
     * @param name the name of the action, should be namespaced to avoid conflicts
     * @param action the action. The function receives a Map as parameters (named parameters) and returns an object of type
     *        {@code T}.
     *        Note that the type {@code T} must be a class shared by both the deployment and the runtime.
     */
    public static <T> void register(String name, BiFunction<Object, Map<String, String>, T> action) {
        assistantActions.put(name, action);
    }

    /**
     * Invokes a registered action
     *
     * @param name the name of the action
     * @return the result of the invocation. An empty map is returned for action not returning any result.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(String name) {
        return DevConsoleManager.invoke(name, Map.of());
    }

    /**
     * Invokes a registered action
     *
     * @param name the name of the action
     * @param params the named parameters
     * @return the result of the invocation. An empty map is returned for action not returning any result.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(String name, Map<String, String> params) {
        var function = actions.get(name);
        if (function == null) {
            // Try assistant actions
            var bifunction = assistantActions.get(name);
            if (bifunction != null) {
                Object assistant = DevConsoleManager.getGlobal(DEV_MANAGER_GLOBALS_ASSISTANT);
                if (assistant != null) {
                    return (T) bifunction.apply(assistant, params);
                } else {
                    throw new RuntimeException("Assistant not available");
                }
            } else {
                throw new NoSuchElementException(name);
            }
        } else {
            return (T) function.apply(params);
        }
    }
}
