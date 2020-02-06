package io.quarkus.kafka.client.health;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.Node;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class KafkaHealthCheck implements HealthCheck {

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    private String bootstrapServers;

    private AdminClient client;

    @PostConstruct
    void init() {
        Map<String, Object> conf = new HashMap<>();
        conf.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        client = AdminClient.create(conf);
    }

    @PreDestroy
    void stop() {
        client.close();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Kafka connection health check").up();
        try {
            StringBuilder nodes = new StringBuilder();
            for (Node node : client.describeCluster().nodes().get()) {
                if (nodes.length() > 0) {
                    nodes.append(',');
                }
                nodes.append(node.host()).append(':').append(node.port());
            }
            return builder.withData("nodes", nodes.toString()).build();
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }
}
