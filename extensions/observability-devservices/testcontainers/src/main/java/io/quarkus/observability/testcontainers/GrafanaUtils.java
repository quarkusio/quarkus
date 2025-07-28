package io.quarkus.observability.testcontainers;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.yaml.snakeyaml.Yaml;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

public class GrafanaUtils {

    static final String META_INF_GRAFANA = "META-INF/grafana";

    static String consumeGrafanaResources(String config, Consumer<String> consumer) {
        return consumeGrafanaResources(config, consumer, findGrafanaDashboards());
    }

    @SuppressWarnings("unchecked")
    // Take dashboards as param, so we can test this
    static String consumeGrafanaResources(String config, Consumer<String> consumer, Set<String> dashboards) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(config);
        List<Map<String, Object>> providers = (List<Map<String, Object>>) data.get("providers");

        dashboards.forEach(s -> {
            String sub = s.substring("grafana-dashboard-".length(), s.length() - ".json".length());
            Map<String, Object> provider = new LinkedHashMap<>();
            String name = toName(sub);
            provider.put("name", name);
            provider.put("type", "file");
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("path", "/otel-lgtm/" + s);
            options.put("foldersFromFilesStructure", false);
            provider.put("options", options);
            providers.add(provider);

            consumer.accept(s);
        });

        StringWriter writer = new StringWriter();
        yaml.dump(data, writer);

        return writer.toString();
    }

    private static String toName(String path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Illegal path: " + path);
        }
        StringBuilder name = new StringBuilder();
        boolean dash = true;
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (dash) {
                ch = Character.toUpperCase(ch);
                dash = false;
            } else {
                if (ch == '-') {
                    dash = true;
                    ch = ' ';
                }
            }
            name.append(ch);
        }
        return name.toString();
    }

    /**
     * Visits all {@code META-INF/grafana} directories and their content found on the runtime classpath
     * and returns all Grafana dashboard json configurations
     */
    private static Set<String> findGrafanaDashboards() {
        Set<String> dashboards = new HashSet<>();
        final List<ClassPathElement> elements = QuarkusClassLoader.getElements(META_INF_GRAFANA, false);
        if (!elements.isEmpty()) {
            for (var element : elements) {
                if (element.isRuntime()) {
                    element.apply(tree -> {
                        tree.walkIfContains(META_INF_GRAFANA, visit -> {
                            Path visitPath = visit.getPath();
                            if (!Files.isDirectory(visitPath)) {
                                String rel = visit.getRelativePath();
                                // Ensure that the relative path starts with the right prefix and suffix before calling substring
                                if (rel.startsWith(META_INF_GRAFANA + "/grafana-dashboard-") && rel.endsWith(".json")) {
                                    // Strip the "META-INF/grafana/" prefix
                                    String subPath = rel.substring(META_INF_GRAFANA.length() + 1);
                                    dashboards.add(subPath);
                                }
                            }
                        });
                        return null;
                    });
                }
            }
        }
        return dashboards;
    }

}
