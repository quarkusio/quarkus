package io.quarkus.websockets.next.deployment.produi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.websockets.next.deployment.Callback;
import io.quarkus.websockets.next.deployment.WebSocketEndpointBuildItem;
import io.quarkus.websockets.next.deployment.WebSocketProcessor;
import io.quarkus.websockets.next.runtime.produi.WebSocketsNextProdUIRecorder;
import io.quarkus.websockets.next.runtime.produi.WebSocketsNextProdUIService;

/**
 * Contributes a read-only Prod UI page listing the registered WebSocket server
 * endpoints and their active connection count. The Dev UI is not reused: its
 * {@code qwc-wsn-endpoints} component is a message-injection console (opening Dev
 * UI connections, sending and clearing messages) and imports dev-only web
 * components. A bespoke read-only component is provided instead. The endpoint
 * metadata is seeded into the runtime service at runtime init; the live
 * connection count is derived from the always-present {@code OpenConnections}
 * bean.
 */
public class WebSocketsNextProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the endpoint gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(WebSocketsNextProdUIService.class);
    }

    @BuildStep
    void createProdUIPage(List<WebSocketEndpointBuildItem> endpoints,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        long serverEndpoints = endpoints.stream().filter(WebSocketEndpointBuildItem::isServer).count();
        if (serverEndpoints == 0) {
            return;
        }

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Server Endpoints")
                .icon("font-awesome-solid:plug")
                .componentLink("pwc-wsn-endpoints.js"));
        prodUIProducer.produce(page);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeProdUIService(WebSocketsNextProdUIRecorder recorder, List<WebSocketEndpointBuildItem> endpoints) {
        List<Map<String, Object>> endpointsData = createEndpointsData(endpoints);
        if (endpointsData.isEmpty()) {
            return;
        }
        recorder.initializeProdUIService(endpointsData);
    }

    private List<Map<String, Object>> createEndpointsData(List<WebSocketEndpointBuildItem> endpoints) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (WebSocketEndpointBuildItem endpoint : endpoints.stream().filter(WebSocketEndpointBuildItem::isServer)
                .sorted(Comparator.comparing(e -> e.path))
                .collect(Collectors.toList())) {
            Map<String, Object> endpointData = new LinkedHashMap<>();
            endpointData.put("clazz", endpoint.bean.getImplClazz().name().toString());
            endpointData.put("endpointId", endpoint.id);
            endpointData.put("path", WebSocketProcessor.getOriginalPath(endpoint.path));
            endpointData.put("executionMode", endpoint.inboundProcessingMode.toString());
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
            endpointData.put("callbacks", callbacks);
            data.add(endpointData);
        }
        return data;
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
