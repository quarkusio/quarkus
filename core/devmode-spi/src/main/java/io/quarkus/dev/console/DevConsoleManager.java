package io.quarkus.dev.console;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.dev.spi.HotReplacementContext;

public class DevConsoleManager {

    private static volatile Consumer<DevConsoleRequest> handler;
    private static volatile Map<String, Map<String, Object>> templateInfo;
    private static volatile HotReplacementContext hotReplacementContext;
    private static volatile Object quarkusBootstrap;

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
}
