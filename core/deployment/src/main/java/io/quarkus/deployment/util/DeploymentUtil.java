package io.quarkus.deployment.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.smallrye.config.Config;

/**
 * These are also called by the Maven and Gradle plugins, outside the augmentation, where there is no configuration
 * registered for the context classloader, so the configuration has to be created if it is not available.
 */
public class DeploymentUtil {

    public static final String DEPLOY = "quarkus.%s.deploy";
    private static final Pattern QUARKUS_DEPLOY_PATTERN = Pattern.compile("quarkus\\.([^\\.]+)\\.deploy");

    /**
     * Get the available deployers.
     * The list is obtained by checking for properties {@code quarkus.xxx.deploy}.
     * These properties have a default value and thus they will be found regardless of the
     * actual user configuration, so we check for property names instead.
     *
     * @return a {@link List} with all available deployers.
     */
    public static List<String> getDeployers() {
        return StreamSupport.stream(Config.getOrCreate().getPropertyNames().spliterator(), false)
                .map(QUARKUS_DEPLOY_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .collect(Collectors.toList());
    }

    /**
     * @return a {@link Predicate} that tests if deployer is enabled.
     */
    public static Predicate<String> isDeployExplicitlyEnabled() {
        return deployer -> Config.getOrCreate().getOptionalValue(String.format(DEPLOY, deployer), Boolean.class)
                .orElse(false);
    }

    /**
     * @return the name of the first deployer that is explicitly enabled
     */
    public static Optional<String> getEnabledDeployer() {
        return getDeployers().stream().filter(isDeployExplicitlyEnabled()).findFirst();
    }

    /**
     * Check if any of the specified deployers are enabled.
     *
     * @param deployers name of the specified deployers.
     * @return {@code true} if the specified deployer is explicitly enabled, {@code false} otherwise.
     */
    public static boolean isDeploymentEnabled(String... deployers) {
        if (deployers == null) {
            return false;
        }
        return getDeployers().stream().filter(isDeployExplicitlyEnabled()).anyMatch(d -> Arrays.asList(deployers).contains(d));
    }

    /**
     * @return true if deployment is explicitly enabled using: {@code quarkus.<deployment target>.deploy=true}.
     */
    public static boolean isDeploymentEnabled() {
        return getEnabledDeployer().isPresent();
    }
}
