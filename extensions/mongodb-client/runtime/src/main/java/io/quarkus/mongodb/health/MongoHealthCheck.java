package io.quarkus.mongodb.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;

@Readiness
@ApplicationScoped
public class MongoHealthCheck implements HealthCheck {
    private static final String DEFAULT_CLIENT = "__default__";
    private Map<String, MongoClient> clients = new HashMap<>();

    @PostConstruct
    protected void init() {
        Set<Bean<?>> beans = Arc.container().beanManager().getBeans(MongoClient.class);
        for (Bean<?> bean : beans) {
            if (bean.getName() == null) {
                // this is the default mongo client: retrieve it by type
                MongoClient defaultClient = Arc.container().instance(MongoClient.class).get();
                clients.put(DEFAULT_CLIENT, defaultClient);
            } else {
                MongoClient client = (MongoClient) Arc.container().instance(bean.getName()).get();
                clients.put(bean.getName(), client);
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("MongoDB connection health check").up();
        for (Map.Entry<String, MongoClient> client : clients.entrySet()) {
            boolean isDefault = DEFAULT_CLIENT.equals(client.getKey());
            MongoClient mongoClient = client.getValue();
            try {
                StringBuilder databases = new StringBuilder();
                for (String db : mongoClient.listDatabaseNames()) {
                    if (databases.length() != 0) {
                        databases.append(", ");
                    }
                    databases.append(db);
                }
                String mongoClientName = isDefault ? "default" : client.getKey();
                builder.up().withData(mongoClientName, databases.toString());
            } catch (Exception e) {
                return builder.down().withData("reason", e.getMessage()).build();
            }
        }
        return builder.build();
    }
}
