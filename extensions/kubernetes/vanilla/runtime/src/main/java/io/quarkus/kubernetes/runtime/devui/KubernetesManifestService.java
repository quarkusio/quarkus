package io.quarkus.kubernetes.runtime.devui;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.dev.console.DevConsoleManager;

@ApplicationScoped
public class KubernetesManifestService {

    public String generateManifests() {
        try {
            Map<String, String> map = DevConsoleManager.invoke("kubernetes-generate-manifest", Map.of());
            return toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private static String toJson(Map<String, String> map) {
        var content = map.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue().replace("\n", "\\n") + "\"")
                .collect(Collectors.joining(","));
        return "{" + content + "}";
    }
}
