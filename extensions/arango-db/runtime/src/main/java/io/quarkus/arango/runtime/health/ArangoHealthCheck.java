package io.quarkus.arango.runtime.health;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.ArangoDBVersion;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Readiness
@ApplicationScoped
public class ArangoHealthCheck implements HealthCheck {

    private static final Logger log = Logger.getLogger(ArangoHealthCheck.class);

    @Inject
    ArangoDB arangoDB;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Arango connection health check").up();
        try {
            ArangoDBVersion version = arangoDB.getVersion();
            return buildStatusUp(version, builder);

        } catch (ArangoDBException e) {
            log.warn(e.getErrorMessage());
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }

    /**
     * Applies the given {@link ArangoDBVersion} to the {@link HealthCheckResponseBuilder builder} and calls {@code build}
     * afterwards.
     *
     * @param arangoDBVersion the result summary returned by the server
     * @param builder         the health builder to be modified
     * @return the final {@link HealthCheckResponse health check response}
     */
    private static HealthCheckResponse buildStatusUp(ArangoDBVersion arangoDBVersion, HealthCheckResponseBuilder builder) {

        builder.withData("server", arangoDBVersion.getServer());
        builder.withData("version", arangoDBVersion.getVersion());


        return builder.build();
    }
}
