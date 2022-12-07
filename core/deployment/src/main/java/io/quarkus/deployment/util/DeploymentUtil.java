package io.quarkus.deployment.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class DeploymentUtil {

    public static final String DEPLOY = "quarkus.%s.deploy";
    private static final Pattern QUARKUS_DEPLOY_PATTERN = Pattern.compile("quarkus\\.([^\\.]+)\\.deploy");

    /**
     * Get the available deployers.
     * The list is obtained by checking for properties `quarkus.xxx.deploy`.
     * These properties have a default value and thus they will be found regardless of the
     * actual user configuration, so we check for property names instead.
     *
     * @return a {@link List} with all available deployers.
     */
    public static List<String> getDeployers() {
        Config config = ConfigProvider.getConfig();
        return Stream.concat(System.getProperties().entrySet().stream().map(e -> String.valueOf(e.getKey())),
                StreamSupport.stream(config.getPropertyNames().spliterator(),
                        false))
                .map(p -> QUARKUS_DEPLOY_PATTERN.matcher(p))
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .collect(Collectors.toList());
    }

    /**
     * @return a {@link Predicate} that tests if deploye is enabled.
     */
    public static Predicate<String> isDeployExplicitlyEnabled() {
        return deployer -> ConfigProvider.getConfig().getOptionalValue(String.format(DEPLOY, deployer), Boolean.class)
                .orElse(false);
    }

    /**
     * @return the name of the first deployer that is explicitly enabled
     */
    public static Optional<String> getEnabledDeployer() {
        return getDeployers().stream().filter(isDeployExplicitlyEnabled()).findFirst();
    }

    /**
     * Check if any of the specified deployers is enabled.
     *
     * @param the name of the speicifed deployers.
     * @return true if the specified deploy is explicitly enabled, fasle otherwise.
     */
    public static boolean isDeploymentEnabled(String... deployer) {
        return getDeployers().stream().filter(isDeployExplicitlyEnabled()).anyMatch(d -> Arrays.asList(deployer).contains(d));
    }

    /**
     * @return true if deployment is explicitly enabled using: `quarkus.<deployment target>.deploy=true`.
     */
    public static boolean isDeploymentEnabled() {
        return getEnabledDeployer().isPresent();
    }
}
