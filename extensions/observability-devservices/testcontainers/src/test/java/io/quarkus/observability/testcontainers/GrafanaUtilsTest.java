package io.quarkus.observability.testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

public class GrafanaUtilsTest {

    protected static final String DASHBOARDS_CONFIG = """
            apiVersion: 1

            providers:
              - name: "Quarkus Micrometer Prometheus registry"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-quarkus-micrometer-prometheus.json
                  foldersFromFilesStructure: false
            """;

    @Test
    public void testGrafanaUtils() {
        String config = GrafanaUtils.consumeGrafanaResources(
                DASHBOARDS_CONFIG,
                s -> {
                    try (InputStream stream = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(GrafanaUtils.META_INF_GRAFANA + "/" + s)) {
                        System.out.println(new String(stream.readAllBytes()));
                        System.out.println("------");
                    } catch (IOException ignored) {
                    }
                },
                Set.of("grafana-dashboard-my-test.json"));

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(config);
        StringWriter writer = new StringWriter();
        yaml.dump(data, writer);
        System.out.println(writer);
    }

}
