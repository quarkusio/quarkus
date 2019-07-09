package io.quarkus.neo4j.runtime;

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
import io.quarkus.runtime.annotations.Template;
import io.quarkus.runtime.ssl.SslContextConfiguration;

@Template
public class Neo4jDriverTemplate {

    private static final Logger log = Logger.getLogger(Neo4jDriverTemplate.class);

    static volatile Driver driver;

    public void configureNeo4jProducer(BeanContainer beanContainer, Neo4jConfiguration configuration,
            ShutdownContext shutdownContext) {

        initializeDriver(configuration, shutdownContext);

        Neo4jDriverProducer driverProducer = beanContainer.instance(Neo4jDriverProducer.class);
        driverProducer.initialize(driver);
    }

    private void initializeDriver(Neo4jConfiguration configuration,
            ShutdownContext shutdownContext) {

        String uri = configuration.uri;
        AuthToken authToken = AuthTokens.none();
        if (!configuration.authentication.disabled) {
            authToken = AuthTokens.basic(configuration.authentication.username, configuration.authentication.password);
        }

        // Disable encryption regardless of user configuration when ssl is not natively enabled.
        Config.ConfigBuilder configBuilder = createBaseConfig(configuration);
        if (ImageInfo.inImageRuntimeCode() && !SslContextConfiguration.isSslNativeEnabled()) {
            log.warn("Native SSL is disabled, communication between this client and the Neo4j server won't be encrypted.");
            configBuilder = configBuilder.withoutEncryption();
        }

        driver = GraphDatabase.driver(uri, authToken, configBuilder.build());
        shutdownContext.addShutdownTask(driver::close);
    }

    private Config.ConfigBuilder createBaseConfig(Neo4jConfiguration neo4jConfiguration) {
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
}
