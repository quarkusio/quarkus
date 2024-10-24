package io.quarkus.kubernetes.deployment;

import static io.quarkus.container.spi.ImageReference.DEFAULT_TAG;
import static io.quarkus.deployment.builditem.ApplicationInfoBuildItem.UNSET_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_TARGET;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.S2I;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.util.DeploymentUtil;

public class KubernetesConfigUtil {

    /**
     * It should be the same name as in VertxHttpProcessor.kubernetesForManagement.
     */
    public static final String MANAGEMENT_PORT_NAME = "management";

    private static final String DEKORATE_PREFIX = "dekorate.";

    /**
     * Get the explicitly configured deployment target, if any.
     * The explicit deployment target is determined using: {@code quarkus.kubernetes.deployment-target=<deployment-target>}
     */
    public static Optional<String> getExplicitlyConfiguredDeploymentTarget() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue(DEPLOYMENT_TARGET, String.class);
    }

    /**
     * @deprecated Use {@link #getExplicitlyConfiguredDeploymentTargets()} instead
     */
    @Deprecated(forRemoval = true)
    public static List<String> getExplictilyDeploymentTargets() {
        return getExplicitlyConfiguredDeploymentTargets();
    }

    /**
     * The explicitly configured deployment target list.
     * The configured deployment targets are determined using: {@code quarkus.kubernetes.deployment-target=<deployment-target>}
     */
    public static List<String> getExplicitlyConfiguredDeploymentTargets() {
        return splitDeploymentTargets(getExplicitlyConfiguredDeploymentTarget());
    }

    private static List<String> splitDeploymentTargets(Optional<String> commaSeparatedDeploymentTargets) {
        return commaSeparatedDeploymentTargets
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * Get the user configured deployment target, if any.
     * The configured deployment target is determined using:
     * <ol>
     * <li>the value of {@code quarkus.kubernetes.deployment-target=<deployment-target>}</li>
     * <li>the presence of {@code quarkus.<deployment-target>.deploy=true}</li>
     * </ol>
     */
    public static Optional<String> getConfiguredDeploymentTarget() {
        return getExplicitlyConfiguredDeploymentTarget().or(DeploymentUtil::getEnabledDeployer);
    }

    /**
     * @deprecated Use {@link #getConfiguredDeploymentTargets()} instead
     */
    @Deprecated(forRemoval = true)
    public static List<String> getConfiguratedDeploymentTargets() {
        return getConfiguredDeploymentTargets();
    }

    /**
     * Get the configured deployment target list as determined by:
     * <ol>
     * <li>the value of {@code quarkus.kubernetes.deployment-target=<deployment-target>}</li>
     * <li>the presence of {@code quarkus.<deployment-target>.deploy=true}</li>
     * </ol>
     */
    public static List<String> getConfiguredDeploymentTargets() {
        return splitDeploymentTargets(getConfiguredDeploymentTarget());
    }

    public static boolean isDeploymentEnabled() {
        return DeploymentUtil.isDeploymentEnabled("kubernetes", "openshift", "knative", "kind", "minikube");
    }

    /*
     * Collects configuration properties for Kubernetes. Reads all properties and
     * matches properties that match known Dekorate generators. These properties may
     * or may not be prefixed with {@code quarkus.} though the prefixed ones take precedence.
     *
     * @return A map containing the properties.
     */
    public static Map<String, Object> toMap(PlatformConfiguration... platformConfigurations) {
        Map<String, Object> result = new HashMap<>();

        // Most of quarkus prefixed properties are handled directly by the config items (KubernetesConfig, OpenshiftConfig, KnativeConfig)
        // We just need group, name & version parsed here, as we don't have decorators for these (low level properties).
        Map<String, Object> quarkusPrefixed = new HashMap<>();

        Arrays.stream(platformConfigurations).forEach(p -> {
            p.partOf().ifPresent(g -> quarkusPrefixed.put(DEKORATE_PREFIX + p.targetPlatformName() + ".part-of", g));
            p.name().ifPresent(n -> quarkusPrefixed.put(DEKORATE_PREFIX + p.targetPlatformName() + ".name", n));
            p.version()
                    .map(v -> v.equals(UNSET_VALUE) ? DEFAULT_TAG : v)
                    .ifPresent(v -> quarkusPrefixed.put(DEKORATE_PREFIX + p.targetPlatformName() + ".version", v));
        });

        result.putAll(quarkusPrefixed);
        result.putAll(toS2iProperties(quarkusPrefixed));
        return result;
    }

    public static boolean managementPortIsEnabled() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.management.enabled", Boolean.class).orElse(false);
    }

    private static Map<String, Object> toS2iProperties(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        map.forEach((k, v) -> {
            if (k.contains(OPENSHIFT)) {
                result.put(k.replaceAll(OPENSHIFT, S2I), v);
            }
        });
        return result;
    }
}
