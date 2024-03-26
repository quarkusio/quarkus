package io.quarkus.websockets.next.deployment.devui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.websockets.next.deployment.GeneratedEndpointBuildItem;
import io.quarkus.websockets.next.deployment.WebSocketEndpointBuildItem;
import io.quarkus.websockets.next.deployment.WebSocketServerProcessor;
import io.quarkus.websockets.next.runtime.devui.WebSocketNextJsonRPCService;

public class WebSocketServerDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void pages(List<WebSocketEndpointBuildItem> endpoints, List<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<CardPageBuildItem> cardPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem.addBuildTimeData("endpoints", createEndpointsJson(endpoints, generatedEndpoints));

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Endpoints")
                .icon("font-awesome-solid:plug")
                .componentLink("qwc-wsn-endpoints.js")
                .staticLabel(String.valueOf(endpoints.size())));

        cardPages.produce(pageBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(WebSocketNextJsonRPCService.class);
    }

    private List<Map<String, Object>> createEndpointsJson(List<WebSocketEndpointBuildItem> endpoints,
            List<GeneratedEndpointBuildItem> generatedEndpoints) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (WebSocketEndpointBuildItem endpoint : endpoints.stream().sorted(Comparator.comparing(e -> e.path))
                .collect(Collectors.toList())) {
            Map<String, Object> endpointJson = new HashMap<>();
            String clazz = endpoint.bean.getImplClazz().name().toString();
            endpointJson.put("clazz", clazz);
            endpointJson.put("generatedClazz",
                    generatedEndpoints.stream().filter(ge -> ge.endpointClassName.equals(clazz)).findFirst()
                            .orElseThrow().generatedClassName);
            endpointJson.put("path", getOriginalPath(endpoint.path));
            endpointJson.put("executionMode", endpoint.executionMode.toString());
            List<Map<String, Object>> callbacks = new ArrayList<>();
            addCallback(endpoint.onOpen, callbacks);
            addCallback(endpoint.onBinaryMessage, callbacks);
            addCallback(endpoint.onTextMessage, callbacks);
            addCallback(endpoint.onPongMessage, callbacks);
            addCallback(endpoint.onClose, callbacks);
            endpointJson.put("callbacks", callbacks);
            json.add(endpointJson);
        }
        return json;
    }

    private void addCallback(WebSocketEndpointBuildItem.Callback callback, List<Map<String, Object>> callbacks) {
        if (callback != null) {
            callbacks.add(Map.of("annotation", callback.annotation.toString(), "method", callback.method.toString()));
        }
    }

    static String getOriginalPath(String path) {
        StringBuilder sb = new StringBuilder();
        Matcher m = WebSocketServerProcessor.TRANSLATED_PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            // Replace :foo with {foo}
            String match = m.group();
            m.appendReplacement(sb, "{" + match.subSequence(1, match.length()) + "}");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
