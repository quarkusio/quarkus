package io.quarkus.neo4j.runtime.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;

@Readiness
@ApplicationScoped
public class Neo4jHealthCheck implements HealthCheck {

    private static final Logger log = Logger.getLogger(Neo4jHealthCheck.class);

    /**
     * The Cypher statement used to verify Neo4j is up.
     */
    private static final String CYPHER = "RETURN 1 AS result";
    /**
     * Message indicating that the health check failed.
     */
    private static final String MESSAGE_HEALTH_CHECK_FAILED = "Neo4j health check failed";
    /**
     * Message logged before retrying a health check.
     */
    private static final String MESSAGE_SESSION_EXPIRED = "Neo4j session has expired, retrying one single time to retrieve server health.";
    /**
     * The default session config to use while connecting.
     */
    private static final SessionConfig DEFAULT_SESSION_CONFIG = SessionConfig.builder()
            .withDefaultAccessMode(AccessMode.WRITE)
            .build();

    @Inject
    Driver driver;

    @Override
    public HealthCheckResponse call() {

        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Neo4j connection health check").up();
        try {
            ResultSummary resultSummary;
            // Retry one time when the session has been expired
            try {
                resultSummary = runHealthCheckQuery();
            } catch (SessionExpiredException sessionExpiredException) {
                log.warn(MESSAGE_SESSION_EXPIRED);
                resultSummary = runHealthCheckQuery();
            }
            return buildStatusUp(resultSummary, builder);
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }

    /**
     * Applies the given {@link ResultSummary} to the {@link HealthCheckResponseBuilder builder} and calls {@code build}
     * afterwards.
     *
     * @param resultSummary the result summary returned by the server
     * @param builder the health builder to be modified
     * @return the final {@link HealthCheckResponse health check response}
     */
    private static HealthCheckResponse buildStatusUp(ResultSummary resultSummary, HealthCheckResponseBuilder builder) {
        ServerInfo serverInfo = resultSummary.server();

        builder.withData("server", serverInfo.version() + "@" + serverInfo.address());

        String databaseName = resultSummary.database().name();
        if (!(databaseName == null || databaseName.trim().isEmpty())) {
            builder.withData("database", databaseName.trim());
        }

        return builder.build();
    }

    private ResultSummary runHealthCheckQuery() {
        // We use WRITE here to make sure UP is returned for a server that supports
        // all possible workloads
        try (Session session = this.driver.session(DEFAULT_SESSION_CONFIG)) {
            ResultSummary resultSummary = session.run(CYPHER).consume();
            return resultSummary;
        }
    }
}
