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
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.internal.retry.ExponentialBackoffRetryLogic;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;

@Readiness
@ApplicationScoped
public class Neo4jHealthCheck implements HealthCheck {

    private static final Logger log = Logger.getLogger(Neo4jHealthCheck.class);

    /**
     * The Cypher statement used to verify Neo4j is up.
     */
    private static final String CYPHER = "CALL dbms.components() YIELD versions, name, edition WHERE name = 'Neo4j Kernel' RETURN edition, versions[0] as version";
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
            // Retry one time when the session has been expired
            try {
                return runHealthCheckQuery(builder);
            } catch (Throwable exception) {
                if (ExponentialBackoffRetryLogic.isRetryable(exception)) {
                    log.warn(MESSAGE_SESSION_EXPIRED);
                    return runHealthCheckQuery(builder);
                }
                throw exception;
            }
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }

    /**
     * Applies the given {@link ResultSummary} to the {@link HealthCheckResponseBuilder builder} and calls {@code build}
     * afterwards.
     *
     * @param editionAndVersion a single record containing edition and version
     * @param resultSummary the result summary returned by the server
     * @param builder the health builder to be modified
     * @return the final {@link HealthCheckResponse health check response}
     */
    private static HealthCheckResponse buildStatusUp(Record editionAndVersion, ResultSummary resultSummary,
            HealthCheckResponseBuilder builder) {
        ServerInfo serverInfo = resultSummary.server();

        builder.withData("server", "Neo4j/" + editionAndVersion.get("version").asString() + "@" + serverInfo.address());

        String databaseName = resultSummary.database().name();
        if (!(databaseName == null || databaseName.trim().isEmpty())) {
            builder.withData("database", databaseName.trim());
        }
        builder.withData("edition", editionAndVersion.get("edition").asString());

        return builder.build();
    }

    private HealthCheckResponse runHealthCheckQuery(HealthCheckResponseBuilder builder) {
        // We use WRITE here to make sure UP is returned for a server that supports
        // all possible workloads
        try (Session session = this.driver.session(DEFAULT_SESSION_CONFIG)) {
            var result = session.run(CYPHER);
            var editionAndVersion = result.single();
            ResultSummary resultSummary = result.consume();
            return buildStatusUp(editionAndVersion, resultSummary, builder);
        }
    }
}
