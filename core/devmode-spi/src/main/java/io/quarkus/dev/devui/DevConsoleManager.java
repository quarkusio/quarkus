package io.quarkus.dev.devui;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.quarkus.dev.spi.HotReplacementContext;

public class DevConsoleManager {

    private static volatile Consumer<DevConsoleRequest> handler;
    private static volatile Map<String, Map<String, Object>> templateInfo;
    private static volatile HotReplacementContext hotReplacementContext;
    private static volatile Object quarkusBootstrap;
    private static volatile boolean doingHttpInitiatedReload;
    /**
     * Global map that can be used to share data betweeen the runtime and deployment side
     * to enable communication.
     * <p>
     * Key names should be namespaced.
     * <p>
     * As the class loaders are different these objects will generally need to implement some kind of common interface
     */
    private static Map<String, Object> globals = new ConcurrentHashMap<>();

    public static void registerHandler(Consumer<DevConsoleRequest> requestHandler) {
        handler = requestHandler;
    }

    public static void sentRequest(DevConsoleRequest request) {
        Consumer<DevConsoleRequest> handler = DevConsoleManager.handler;
        if (handler == null) {
            request.getResponse().complete(new DevConsoleResponse(503, Collections.emptyMap(), new byte[0])); //service unavailable
        } else {
            handler.accept(request);
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
        globals.clear();
    }
}
