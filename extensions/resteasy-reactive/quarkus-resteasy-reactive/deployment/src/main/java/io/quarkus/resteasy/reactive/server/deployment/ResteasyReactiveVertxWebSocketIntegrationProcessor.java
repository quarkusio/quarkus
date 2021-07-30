package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.reactive.server.runtime.websocket.VertxWebSocketParamExtractor;
import io.quarkus.resteasy.reactive.server.runtime.websocket.VertxWebSocketRestHandler;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.vertx.core.http.ServerWebSocket;

public class ResteasyReactiveVertxWebSocketIntegrationProcessor {

    static final DotName SERVER_WEB_SOCKET = DotName.createSimple(ServerWebSocket.class.getName());
    public static final String NAME = ResteasyReactiveVertxWebSocketIntegrationProcessor.class.getName();

    @BuildStep
    MethodScannerBuildItem scanner() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                if (methodContext.containsKey(NAME)) {
                    return Collections.singletonList(new VertxWebSocketRestHandler());
                }
                return Collections.emptyList();
            }

            @Override
            public ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
                    boolean field, Map<String, Object> methodContext) {
                if (paramType.name().equals(SERVER_WEB_SOCKET)) {
                    methodContext.put(NAME, true);
                    return new VertxWebSocketParamExtractor();
                }
                return null;
            }

            @Override
            public boolean isMethodSignatureAsync(MethodInfo info) {
                for (var param : info.parameters()) {
                    if (param.name().equals(SERVER_WEB_SOCKET)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
