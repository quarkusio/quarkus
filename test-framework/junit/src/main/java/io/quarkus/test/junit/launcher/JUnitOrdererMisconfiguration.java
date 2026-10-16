package io.quarkus.test.junit.launcher;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.ClassOrderer;

import io.quarkus.test.config.QuarkusClassOrderer;
import io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer;

/**
 * Decides whether the {@code junit.jupiter.testclass.order.default} value seen by the JUnit launcher is one that Quarkus can
 * work with, and if not, explains how to fix it.
 * <p>
 * Surefire and Failsafe 3.6.0 and later translate {@code runOrder} into this JUnit parameter and pass it explicitly to the
 * launcher, where it takes precedence over any {@code junit-platform.properties} file or system property.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57084">quarkusio/quarkus#57084</a>
 * @see <a href="https://github.com/apache/maven-surefire/issues/3529">apache/maven-surefire#3529</a>
 */
final class JUnitOrdererMisconfiguration {

    static final String ORDERER_PROPERTY = "junit.jupiter.testclass.order.default";

    private static final String UNDOCUMENTED_SECONDARY_ORDERER_PROPERTY = "junit.quarkus.orderer.secondary-orderer";

    private static final String SECONDARY_ORDERER_PROPERTY = "quarkus.test.class-orderer";

    private static final Set<String> ACCEPTED_ORDERERS = Set.of(
            QuarkusTestProfileAwareClassOrderer.class.getName(),
            QuarkusClassOrderer.class.getName());

    private static final Set<String> JUNIT_RUN_ORDER_ORDERERS = Set.of(
            ClassOrderer.ClassName.class.getName(),
            ClassOrderer.Random.class.getName());

    private static final String SUREFIRE_REVERSE_ORDERER = "org.apache.maven.surefire.junitplatform.ReverseOrdering$ReverseClassOrder";

    private static final String GENERAL_EXPLANATION = """
            Critical failure. Quarkus tests would fail with corrupted application errors.
            The reason is that they would not run in the right order, because the Quarkus JUnit configuration has been overridden.
            When there are multiple test profiles or resources, Quarkus uses a JUnit ClassOrderer to sort tests so that test with the same profile run one after each other.
            Running tests with a different sorting risks tests running on an application that has been cleaned up.
            """;

    private JUnitOrdererMisconfiguration() {
    }

    /**
     * @param configuredOrderer the value of {@code junit.jupiter.testclass.order.default} in the discovery request
     * @return a failure message, or empty if the configured orderer is compatible with Quarkus
     */
    static Optional<String> failureMessage(Optional<String> configuredOrderer) {
        if (configuredOrderer.isPresent() && ACCEPTED_ORDERERS.contains(configuredOrderer.get())) {
            return Optional.empty();
        }
        if (configuredOrderer.isEmpty()) {
            return Optional.of(missingOrdererMessage());
        }
        String orderer = configuredOrderer.get();
        if (JUNIT_RUN_ORDER_ORDERERS.contains(orderer) || SUREFIRE_REVERSE_ORDERER.equals(orderer)) {
            return Optional.of(surefireRunOrderMessage(orderer));
        }
        return Optional.of(customOrdererMessage(orderer));
    }

    private static String missingOrdererMessage() {
        return String.format(
                """
                        %sIt looks like there is a junit-platform.properties configuration file on the project classpath.
                        The JUnit framework will only read the first properties it finds, which prevents Quarkus from setting the class orderer it needs.
                        Please either add %s=%s to your project's junit-platform.properties file, or remove the file and use system properties to configure JUnit.""",
                GENERAL_EXPLANATION, ORDERER_PROPERTY, QuarkusTestProfileAwareClassOrderer.class.getName());
    }

    private static String surefireRunOrderMessage(String orderer) {
        String secondaryOrdererAdvice = SUREFIRE_REVERSE_ORDERER.equals(orderer)
                ? "That orderer only exists inside a Surefire fork, so it cannot be used as a secondary orderer; write your own ClassOrderer and set it with "
                        + SECONDARY_ORDERER_PROPERTY + " instead."
                : "To keep that order, set " + SECONDARY_ORDERER_PROPERTY + "=" + orderer + " in application.properties.";
        return String.format(
                """
                        %sThe class orderer %s was passed to JUnit explicitly. This usually happens because the runOrder option of maven-surefire-plugin or maven-failsafe-plugin (3.6.0 and later) is set to alphabetical, random or reversealphabetical. It can also come from a junit-platform.properties file or a system property.
                        An explicitly passed orderer takes precedence over anything Quarkus can configure, so %s has no effect in this case.
                        Please either remove the runOrder option from the plugin configuration, or keep it and set %s=%s in the plugin's configurationParameters (inside <properties>), which takes precedence over runOrder. If the orderer comes from junit-platform.properties or a system property, remove that entry instead. Note that runOrder=random also randomizes the order of test methods. %s""",
                GENERAL_EXPLANATION, orderer, UNDOCUMENTED_SECONDARY_ORDERER_PROPERTY, ORDERER_PROPERTY,
                QuarkusTestProfileAwareClassOrderer.class.getName(), secondaryOrdererAdvice);
    }

    private static String customOrdererMessage(String orderer) {
        return String.format(
                "%sTo set a test order while preserving the Quarkus required sorting, please use the Quarkus configuration to set %s=%s, and remove the junit-platform.properties (if any) from the classpath.",
                GENERAL_EXPLANATION, SECONDARY_ORDERER_PROPERTY, orderer);
    }
}
