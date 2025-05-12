package io.quarkus.websockets.next.deployment.devui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.websockets.next.deployment.Callback;
import io.quarkus.websockets.next.deployment.GeneratedEndpointBuildItem;
import io.quarkus.websockets.next.deployment.WebSocketEndpointBuildItem;
import io.quarkus.websockets.next.deployment.WebSocketProcessor;
import io.quarkus.websockets.next.runtime.dev.ui.WebSocketNextJsonRPCService;

public class WebSocketServerDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public void pages(List<WebSocketEndpointBuildItem> endpoints, List<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<CardPageBuildItem> cardPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem.addBuildTimeData("endpoints", createEndpointsJson(endpoints, generatedEndpoints));

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Server Endpoints")
                .icon("font-awesome-solid:plug")
                .componentLink("qwc-wsn-endpoints.js")
                .staticLabel(String.valueOf(endpoints.stream().filter(WebSocketEndpointBuildItem::isServer).count())));

        cardPages.produce(pageBuildItem);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(WebSocketNextJsonRPCService.class);
    }

    private List<Map<String, Object>> createEndpointsJson(List<WebSocketEndpointBuildItem> endpoints,
            List<GeneratedEndpointBuildItem> generatedEndpoints) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (WebSocketEndpointBuildItem endpoint : endpoints.stream().filter(WebSocketEndpointBuildItem::isServer)
                .sorted(Comparator.comparing(e -> e.path))
                .collect(Collectors.toList())) {
            Map<String, Object> endpointJson = new HashMap<>();
            String clazz = endpoint.bean.getImplClazz().name().toString();
            endpointJson.put("clazz", clazz);
            endpointJson.put("generatedClazz",
                    generatedEndpoints.stream().filter(ge -> ge.endpointClassName.equals(clazz)).findFirst()
                            .orElseThrow().generatedClassName);
            endpointJson.put("path", WebSocketProcessor.getOriginalPath(endpoint.path));
            endpointJson.put("executionMode", endpoint.inboundProcessingMode.toString());
            List<Map<String, Object>> callbacks = new ArrayList<>();
            addCallback(endpoint.onOpen, callbacks);
            addCallback(endpoint.onBinaryMessage, callbacks);
            addCallback(endpoint.onTextMessage, callbacks);
            addCallback(endpoint.onPingMessage, callbacks);
            addCallback(endpoint.onPongMessage, callbacks);
            addCallback(endpoint.onClose, callbacks);
            for (Callback c : endpoint.onErrors) {
                addCallback(c, callbacks);
            }
            endpointJson.put("callbacks", callbacks);
            json.add(endpointJson);
        }
        return json;
    }

    private void addCallback(Callback callback, List<Map<String, Object>> callbacks) {
        if (callback != null) {
            callbacks.add(Map.of("annotation", callback.annotation.toString(), "method", methodToString(callback.method)));
        }
    }

    private String methodToString(MethodInfo method) {
        StringBuilder builder = new StringBuilder();
        builder.append(typeToString(method.returnType())).append(' ').append(method.name()).append('(');
        for (Iterator<MethodParameterInfo> it = method.parameters().iterator(); it.hasNext();) {
            MethodParameterInfo p = it.next();
            builder.append(typeToString(p.type()));
            builder.append(' ');
            builder.append(p.name() != null ? p.name() : "arg" + p.position());
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(')');
        if (!method.exceptions().isEmpty()) {
            builder.append(" throws ");
            for (Iterator<Type> it = method.exceptions().iterator(); it.hasNext();) {
                builder.append(typeToString(it.next()));
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
        }
        return builder.toString();
    }

    private String typeToString(Type type) {
        if (type.kind() == Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = type.asParameterizedType();
            StringBuilder builder = new StringBuilder();
            builder.append(parameterizedType.name().withoutPackagePrefix());
            if (!parameterizedType.arguments().isEmpty()) {
                builder.append('<');
                for (Iterator<Type> it = parameterizedType.arguments().iterator(); it.hasNext();) {
                    builder.append(typeToString(it.next()));
                    if (it.hasNext()) {
                        builder.append(", ");
                    }
                }
                builder.append('>');
            }
            return builder.toString();
        } else {
            return type.name().withoutPackagePrefix();
        }
    }

}
