package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.dekorate.kubernetes.config.DeploymentStrategy;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Kubernetes
 */
@ConfigMapping(prefix = "quarkus.kubernetes")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KubernetesConfig extends PlatformConfiguration {

    @Override
    default String targetPlatformName() {
        return Constants.KUBERNETES;
    }

    /**
     * The target deployment platform. Defaults to kubernetes. Can be kubernetes, openshift, knative, minikube etc.,
     * or any combination of the above as comma separated list.
     */
    Optional<List<String>> deploymentTarget();

    /**
     * Specifies the deployment strategy.
     */
    @WithDefault("None")
    DeploymentStrategy strategy();

    /**
     * Specifies rolling update configuration. The configuration is applied when DeploymentStrategy == RollingUpdate, or
     * when explicit configuration has been provided. In the later case RollingUpdate is assumed.
     */
    RollingUpdateConfig rollingUpdate();

    /**
     * The number of desired pods
     */
    @WithDefault("1")
    Integer replicas();

    /**
     * The nodePort to set when serviceType is set to node-port.
     */
    OptionalInt nodePort();

    /**
     * Ingress configuration
     */
    IngressConfig ingress();

    /**
     * Optionally set directory generated Kubernetes resources will be written to. Default is `target/kubernetes`.
     */
    Optional<String> outputDirectory();

    /**
     * If set to true, Quarkus will attempt to deploy the application to the target Kubernetes cluster
     */
    @WithDefault("false")
    boolean deploy();
}
