package io.quarkus.kafka.client.health;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.common.Node;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;

@Readiness
@ApplicationScoped
public class KafkaHealthCheck implements HealthCheck {

    KafkaAdminClient kafkaAdminClient;

    public KafkaHealthCheck(KafkaAdminClient kafkaAdminClient) {
        this.kafkaAdminClient = kafkaAdminClient;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Kafka connection health check").up();
        try {
            StringBuilder nodes = new StringBuilder();
            for (Node node : kafkaAdminClient.getCluster().nodes().get()) {
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
