package io.quarkus.mongodb.health;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;

import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;

@Readiness
@ApplicationScoped
public class MongoHealthCheck implements HealthCheck {

    public static final String CLIENT_DEFAULT = "<default>";
    public static final String CLIENT_DEFAULT_REACTIVE = "<default-reactive>";

    private Map<String, MongoClient> clients = new HashMap<>();
    private Map<String, ReactiveMongoClient> reactiveClients = new HashMap<>();

    @PostConstruct
    protected void init() {
        for (InstanceHandle<MongoClient> handle : Arc.container().select(MongoClient.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getMongoClientName(handle.getBean());
            clients.put(clientName == null ? CLIENT_DEFAULT : clientName, handle.get());
        }
        // reactive clients
        for (InstanceHandle<ReactiveMongoClient> handle : Arc.container()
                .select(ReactiveMongoClient.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getMongoClientName(handle.getBean());
            reactiveClients.put(clientName == null ? CLIENT_DEFAULT_REACTIVE : clientName, handle.get());
        }
    }

    /**
     * Get mongoClient name if defined.
     *
     * @param bean
     * @return mongoClient name or null if not defined
     * @see MongoClientName
     */
    private String getMongoClientName(Bean bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof MongoClientName) {
                return ((MongoClientName) qualifier).value();
            }
        }
        return null;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("MongoDB connection health check").up();
        Document command = new Document("ping", 1);
        for (Map.Entry<String, MongoClient> client : clients.entrySet()) {
            try {
                Document document = client.getValue().getDatabase("admin").runCommand(command);
                builder.up().withData(client.getKey(), document.toJson());
            } catch (Exception e) {
                return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e.getMessage()).build();
            }
        }
        for (Map.Entry<String, ReactiveMongoClient> client : reactiveClients.entrySet()) {
            try {
                Document document = client.getValue().getDatabase("admin").runCommand(command).await().indefinitely();
                builder.up().withData(client.getKey(), document.toJson());
            } catch (Exception e) {
                return builder.down().withData("reason", "reactive client [" + client.getKey() + "]: " + e.getMessage())
                        .build();
            }
        }
        return builder.build();
    }
}
