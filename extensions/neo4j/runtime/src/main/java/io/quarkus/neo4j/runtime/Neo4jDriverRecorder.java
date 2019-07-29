package io.quarkus.neo4j.runtime;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.logging.Level;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.ssl.SslContextConfiguration;

@Recorder
public class Neo4jDriverRecorder {

    private static final Logger log = Logger.getLogger(Neo4jDriverRecorder.class);

    public void configureNeo4jProducer(BeanContainer beanContainer, Neo4jConfiguration configuration,
            ShutdownContext shutdownContext) {

        Driver driver = initializeDriver(configuration, shutdownContext);

        Neo4jDriverProducer driverProducer = beanContainer.instance(Neo4jDriverProducer.class);
        driverProducer.initialize(driver);
    }

    private Driver initializeDriver(Neo4jConfiguration configuration,
            ShutdownContext shutdownContext) {

        String uri = configuration.uri;
        AuthToken authToken = AuthTokens.none();
        if (!configuration.authentication.disabled) {
            authToken = AuthTokens.basic(configuration.authentication.username, configuration.authentication.password);
        }

        Config.ConfigBuilder configBuilder = createBaseConfig();
        configureSsl(configBuilder);
        configurePoolSettings(configBuilder, configuration.pool);

        Driver driver = GraphDatabase.driver(uri, authToken, configBuilder.build());
        shutdownContext.addShutdownTask(driver::close);
        return driver;
    }

    private static Config.ConfigBuilder createBaseConfig() {
        Config.ConfigBuilder configBuilder = Config.builder();
        Logging logging;
        try {
            logging = Logging.slf4j();
        } catch (Exception e) {
            logging = Logging.javaUtilLogging(Level.INFO);
        }
        configBuilder.withLogging(logging);
        return configBuilder;
    }

    private static void configureSsl(Config.ConfigBuilder configBuilder) {

        // Disable encryption regardless of user configuration when ssl is not natively enabled.
        if (ImageInfo.inImageRuntimeCode() && !SslContextConfiguration.isSslNativeEnabled()) {
            log.warn(
                    "Native SSL is disabled, communication between this client and the Neo4j server won't be encrypted.");
            configBuilder.withoutEncryption();
        }
    }

    private static void configurePoolSettings(Config.ConfigBuilder configBuilder, Neo4jConfiguration.Pool pool) {

        if (log.isDebugEnabled()) {
            log.debug("Configuring Neo4j pool settings with " + pool);
        }

        if (pool.logLeakedSessions) {
            configBuilder.withLeakedSessionsLogging();
        }

        configBuilder.withMaxConnectionPoolSize(pool.maxConnectionPoolSize);
        configBuilder.withConnectionLivenessCheckTimeout(pool.idleTimeBeforeConnectionTest.toMillis(), MILLISECONDS);
        configBuilder.withMaxConnectionLifetime(pool.maxConnectionLifetime.toMillis(), MILLISECONDS);
        configBuilder.withConnectionAcquisitionTimeout(pool.connectionAcquisitionTimeout.toMillis(), MILLISECONDS);

        if (pool.metricsEnabled) {
            configBuilder.withDriverMetrics();
        } else {
            configBuilder.withoutDriverMetrics();
        }
    }
}
