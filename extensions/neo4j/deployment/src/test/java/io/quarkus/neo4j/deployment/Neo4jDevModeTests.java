package io.quarkus.neo4j.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.logging.LogRecord;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.test.QuarkusUnitTest;

public class Neo4jDevModeTests {

    @Testcontainers(disabledWithoutDocker = true)
    static class DevServicesShouldStartNeo4j {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
                .setLogRecordPredicate(record -> true)
                .withConfigurationResource("application.properties")
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .contains("Dev Services started a Neo4j container reachable at %s."));

        @Inject
        Driver driver;

        @Test
        public void shouldBeAbleToConnect() {

            assertThatNoException().isThrownBy(() -> driver.verifyConnectivity());

        }
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class WorkingWithDifferentImageAndAdditionalEnv {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
                .setLogRecordPredicate(record -> true)
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.neo4j.devservices.image-name", "neo4j:4.3-enterprise")
                .overrideConfigKey("quarkus.neo4j.devservices.additional-env.NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .contains("Dev Services started a Neo4j container reachable at %s."));

        @Inject
        Driver driver;

        @Test
        public void shouldBeAbleToConnect() {

            assertThatNoException().isThrownBy(() -> driver.verifyConnectivity());
            try (var session = driver.session()) {
                var cypher = "CALL dbms.components() YIELD versions, name, edition WHERE name = 'Neo4j Kernel' RETURN edition, versions[0] as version";
                var result = session.run(cypher).single();
                assertThat(result.get("edition").asString()).isEqualToIgnoringCase("enterprise");
            }
        }
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class WithLocallyDisabledDevServicesTest {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
                .setLogRecordPredicate(record -> true)
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.neo4j.devservices.enabled", "false")
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .contains("Not starting Dev Services for Neo4j, as it has been disabled in the config."));

        @Inject
        Driver driver;

        @Test
        public void shouldNotBeAbleToConnect() {

            assertThatExceptionOfType(ServiceUnavailableException.class).isThrownBy(() -> driver.verifyConnectivity());
        }
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class WithExplicitProperty {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
                .setLogRecordPredicate(record -> true)
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.neo4j.uri", "bolt://localhost:7687")
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .contains("Not starting Dev Services for Neo4j, as there is explicit configuration present."));

        @Inject
        Driver driver;

        @Test
        public void shouldNotBeAbleToConnect() {

            assertThatExceptionOfType(ServiceUnavailableException.class).isThrownBy(() -> driver.verifyConnectivity());
        }
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class WithAlreadyReachableInstance {

        static {
            // Make our check think that
            System.setProperty("quarkus.neo4j.devservices.assumeBoltIsReachable", "true");
        }

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
                .setLogRecordPredicate(record -> true)
                .withConfigurationResource("application.properties")
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .contains("Not starting Dev Services for Neo4j, as the default config points to a reachable address."));

        @Inject
        Driver driver;

        @Test
        public void shouldNotBeAbleToConnect() {

            assertThatExceptionOfType(ServiceUnavailableException.class).isThrownBy(() -> driver.verifyConnectivity());
        }
    }
}
