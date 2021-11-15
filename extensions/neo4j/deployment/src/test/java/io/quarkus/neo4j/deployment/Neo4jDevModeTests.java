package io.quarkus.neo4j.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.function.Predicate;
import java.util.logging.LogRecord;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.test.QuarkusUnitTest;

public class Neo4jDevModeTests {

    @Testcontainers(disabledWithoutDocker = true)
    static class DevServicesShouldStartNeo4jTest {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .withEmptyApplication()
                .withConfigurationResource("application.properties")
                .setLogRecordPredicate(record -> true)
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .contains("Dev Services started a Neo4j container reachable at %s."));

        @Inject
        Driver driver;

        @Test
        public void shouldBeAbleToConnect() {

            assertThatNoException().isThrownBy(() -> driver.verifyConnectivity());
        }
    }

    static Predicate<LogRecord> recordMatches(String message, String port) {
        return r -> message.equals(r.getMessage()) && r.getParameters().length > 0
                && r.getParameters()[0] instanceof String && ((String) r.getParameters()[0]).contains(port);
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class DevServicesShouldBeAbleToUseFixedPorts {

        // Let it burn when there's no free port
        static final String FIXED_BOLD_PORT = PortUtils.findFreePort().get().toString();
        static final String FIXED_HTTP_PORT = PortUtils.findFreePort().get().toString();

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .withEmptyApplication()
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.neo4j.devservices.fixed-bolt-port", FIXED_BOLD_PORT)
                .overrideConfigKey("quarkus.neo4j.devservices.fixed-http-port", FIXED_HTTP_PORT)
                .setAllowTestClassOutsideDeployment(true) // Needed to use the PortUtils above.
                .setLogRecordPredicate(record -> true)
                .assertLogRecords(records -> assertThat(records)
                        .isNotEmpty()
                        .anyMatch(recordMatches("Dev Services started a Neo4j container reachable at %s.", FIXED_BOLD_PORT))
                        .anyMatch(recordMatches("Neo4j Browser is reachable at %s.", FIXED_HTTP_PORT)));

        @Inject
        Driver driver;

        @Test
        public void shouldBeAbleToConnect() {

            assertThatNoException().isThrownBy(() -> driver.verifyConnectivity());
        }
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class DevServicesShouldNotFailWhen7474IsUsed {

        static ServerSocket blockerFor7474;

        static void block7474() {

            if (PortUtils.isFree(7474)) {
                try {
                    blockerFor7474 = new ServerSocket(7474);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                blockerFor7474 = null;
            }
        }

        static void unblock7474() {

            if (blockerFor7474 != null && !blockerFor7474.isClosed()) {
                try {
                    blockerFor7474.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .withEmptyApplication()
                .withConfigurationResource("application.properties")
                .setBeforeAllCustomizer(DevServicesShouldNotFailWhen7474IsUsed::block7474)
                .setAfterAllCustomizer(DevServicesShouldNotFailWhen7474IsUsed::unblock7474)
                .setLogRecordPredicate(record -> true)
                .assertLogRecords(records -> assertThat(records)
                        .isNotEmpty()
                        .noneMatch(recordMatches("Neo4j Browser is reachable at %s.", "7474")));

        @Inject
        Driver driver;

        @Test
        public void shouldBeAbleToConnect() {

            assertThatNoException().isThrownBy(() -> driver.verifyConnectivity());
        }
    }

    @Testcontainers(disabledWithoutDocker = true)
    static class WorkingWithDifferentImageAndAdditionalEnvTest {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .withEmptyApplication()
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
                .withEmptyApplication()
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
    static class WithExplicitPropertyTest {

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .withEmptyApplication()
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
    static class WithAlreadyReachableInstanceTest {

        static {
            // Make our check think that bolt is locally reachable
            System.setProperty("io.quarkus.neo4j.deployment.devservices.assumeBoltIsReachable", "true");
        }

        @RegisterExtension
        static QuarkusUnitTest test = new QuarkusUnitTest()
                .withEmptyApplication()
                .setLogRecordPredicate(record -> true)
                .withConfigurationResource("application.properties")
                .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                        .anyMatch(s -> s.startsWith(
                                "Not starting Dev Services for Neo4j, as the default config points to a reachable address.")));

        @AfterAll
        static void deleteSystemPropertyAgain() {
            System.setProperty("io.quarkus.neo4j.deployment.devservices.assumeBoltIsReachable", "");
        }

        @Inject
        Driver driver;

        @Test
        public void shouldNotBeAbleToConnect() {

            assertThatExceptionOfType(ServiceUnavailableException.class).isThrownBy(() -> driver.verifyConnectivity());
        }
    }
}
