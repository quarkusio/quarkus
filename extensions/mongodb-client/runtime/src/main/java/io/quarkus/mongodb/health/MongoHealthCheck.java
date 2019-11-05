package io.quarkus.mongodb.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.mongodb.client.MongoClient;

@Readiness
@ApplicationScoped
public class MongoHealthCheck implements HealthCheck {
    @Inject
    MongoClient mongoClient;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("MongoDB connection health check").up();
        try {
            StringBuilder databases = new StringBuilder();
            for (String db : mongoClient.listDatabaseNames()) {
                if (databases.length() != 0) {
                    databases.append(", ");
                }
                databases.append(db);
            }
            return builder.withData("databases", databases.toString()).build();
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }
}
