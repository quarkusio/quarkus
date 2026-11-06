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
 * Surefire and Failsafe translate {@code runOrder} into this JUnit parameter and pass it explicitly to the launcher, where it
 * takes precedence over any {@code junit-platform.properties} file or system property. In 3.6.0 this happens even when an
 * orderer is already configured, which replaces the Quarkus one. From 3.6.1 it only happens when no orderer is configured,
 * for example when a {@code junit-platform.properties} file without an orderer hides the Quarkus one.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57084">quarkusio/quarkus#57084</a>
 * @see <a href="https://github.com/apache/maven-surefire/issues/3529">apache/maven-surefire#3529</a>
 * @see <a href="https://github.com/apache/maven-surefire/pull/3530">apache/maven-surefire#3530</a>
 */
final class JUnitOrdererMisconfiguration {

    static final String ORDERER_PROPERTY = "junit.jupiter.testclass.order.default";

    private static final String METHOD_ORDERER_PROPERTY = "junit.jupiter.testmethod.order.default";

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
     * @return whether the configured orderer is missing or is not one that Quarkus can work with
     */
    static boolean isOrderMisconfigured(Optional<String> configuredOrderer) {
        return configuredOrderer.isEmpty() || !ACCEPTED_ORDERERS.contains(configuredOrderer.get());
    }

    /**
     * @param configuredOrderer the value of {@code junit.jupiter.testclass.order.default} in the discovery request, which is
     *        {@linkplain #isOrderMisconfigured(Optional) misconfigured}
     * @return a message explaining how to fix the configuration
     */
    static String failureMessage(Optional<String> configuredOrderer) {
        if (configuredOrderer.isEmpty()) {
            return missingOrdererMessage();
        }
        String orderer = configuredOrderer.get();
        if (JUNIT_RUN_ORDER_ORDERERS.contains(orderer) || SUREFIRE_REVERSE_ORDERER.equals(orderer)) {
            return surefireRunOrderMessage(orderer);
        }
        return customOrdererMessage(orderer);
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
                        %sThe class orderer %s was passed to JUnit explicitly. This usually happens because the runOrder option of maven-surefire-plugin or maven-failsafe-plugin is set to alphabetical, random or reversealphabetical. Version 3.6.0 of these plugins lets runOrder replace the Quarkus orderer; from 3.6.1, runOrder only applies when no orderer is configured, for example when a junit-platform.properties file without an orderer hides the Quarkus one. It can also come from a junit-platform.properties file or a system property.
                        An explicitly passed orderer takes precedence over anything Quarkus can configure, so %s has no effect in this case.
                        If you use version 3.6.0, upgrade to 3.6.1 or later. Otherwise, either remove the runOrder option from the plugin configuration, or keep it and set %s=%s in the plugin's configurationParameters (inside <properties>), which takes precedence over runOrder. If the orderer comes from junit-platform.properties or a system property, remove that entry instead. %s Note that runOrder also sets the order of test methods; if you remove it, set %s in the plugin's configurationParameters to keep that order.""",
                GENERAL_EXPLANATION, orderer, UNDOCUMENTED_SECONDARY_ORDERER_PROPERTY, ORDERER_PROPERTY,
                QuarkusTestProfileAwareClassOrderer.class.getName(), secondaryOrdererAdvice, METHOD_ORDERER_PROPERTY);
    }

    private static String customOrdererMessage(String orderer) {
        return String.format(
                "%sTo set a test order while preserving the Quarkus required sorting, please use the Quarkus configuration to set %s=%s, and remove the junit-platform.properties (if any) from the classpath.",
                GENERAL_EXPLANATION, SECONDARY_ORDERER_PROPERTY, orderer);
    }
}
