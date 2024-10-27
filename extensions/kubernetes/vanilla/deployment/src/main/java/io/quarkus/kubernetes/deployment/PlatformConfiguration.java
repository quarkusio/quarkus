package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface PlatformConfiguration extends EnvVarHolder {
    /**
     * The name of the group this component belongs too.
     */
    Optional<String> partOf();

    /**
     * The name of the application. This value will be used for naming Kubernetes resources like: - Deployment -
     * Service and so on ...
     */
    Optional<String> name();

    /**
     * The version of the application.
     */
    Optional<String> version();

    /**
     * The namespace the generated resources should belong to. If not value is set, then the 'namespace' field will not
     * be added to the 'metadata' section of the generated manifests. This in turn means that when the manifests are
     * applied to a cluster, the namespace will be resolved from the current Kubernetes context (see
     * <a href=
     * "https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/#context">organize-cluster-access-kubeconfig</a>
     * for more details).
     */
    Optional<String> namespace();

    /**
     * Custom labels to add to all resources.
     */
    @ConfigDocMapKey("label-name")
    Map<String, String> labels();

    /**
     * Custom annotations to add to all resources.
     */
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations();

    /**
     * The type of service that will be generated for the application
     */
    @WithDefault("ClusterIP")
    ServiceType serviceType();

    /**
     * Whether to add the build timestamp to the Kubernetes annotations This is a very useful way to have manifests of
     * successive builds of the same application differ - thus ensuring that Kubernetes will apply the updated
     * resources.
     */
    @WithDefault("true")
    boolean addBuildTimestamp();

    /**
     * If <code>true</code>, the 'app.kubernetes.io/version' label will be part of the selectors of Service and
     * Deployment.
     */
    @WithDefault("true")
    boolean addVersionToLabelSelectors();

    /**
     * If <code>true</code>, the 'app.kubernetes.io/name' label will be part of the selectors of Service and Deployment.
     */
    @WithDefault("true")
    boolean addNameToLabelSelectors();

    /**
     * Working directory.
     */
    Optional<String> workingDir();

    /**
     * The commands.
     */
    Optional<List<String>> command();

    /**
     * The arguments.
     */
    Optional<List<String>> arguments();

    /**
     * The service account.
     */
    Optional<String> serviceAccount();

    /**
     * If set, it will change the name of the container according to the configuration.
     */
    Optional<String> containerName();

    /**
     * The application ports.
     */
    Map<String, PortConfig> ports();

    /**
     * Image pull policy.
     */
    @WithDefault("Always")
    ImagePullPolicy imagePullPolicy();

    /**
     * The image pull secret.
     */
    Optional<List<String>> imagePullSecrets();

    /**
     * Enable generation of image pull secret, when the container image username and password are provided.
     */
    @WithDefault("false")
    boolean generateImagePullSecret();

    /**
     * The liveness probe.
     */
    ProbeConfig livenessProbe();

    /**
     * The readiness probe.
     */
    ProbeConfig readinessProbe();

    /**
     * The startup probe.
     */
    ProbeConfig startupProbe();

    /**
     * Prometheus configuration.
     */
    PrometheusConfig prometheus();

    /**
     * Volume mounts.
     */
    Map<String, MountConfig> mounts();

    /**
     * Secret volumes.
     */
    Map<String, SecretVolumeConfig> secretVolumes();

    /**
     * ConfigMap volumes.
     */
    Map<String, ConfigMapVolumeConfig> configMapVolumes();

    /**
     * EmptyDir volumes.
     */
    Optional<List<String>> emptyDirVolumes();

    /**
     * Git Repository volumes.
     */
    Map<String, GitRepoVolumeConfig> gitRepoVolumes();

    /**
     * Persistent Volume Claim volumes.
     */
    Map<String, PvcVolumeConfig> pvcVolumes();

    /**
     * AWS Elastic BlockStore volumes.
     */
    Map<String, AwsElasticBlockStoreVolumeConfig> awsElasticBlockStoreVolumes();

    /**
     * Azure file volumes.
     */
    Map<String, AzureFileVolumeConfig> azureFileVolumes();

    /**
     * Azure disk volumes.
     */
    Map<String, AzureDiskVolumeConfig> azureDiskVolumes();

    /**
     * Init containers.
     */
    Map<String, ContainerConfig> initContainers();

    /**
     * Sidecar containers.
     */
    Map<String, ContainerConfig> sidecars();

    /**
     * The host aliases.
     */
    @WithName("hostaliases")
    Map<String, HostAliasConfig> hostAliases();

    /**
     * The nodeSelector.
     */
    Optional<NodeSelectorConfig> nodeSelector();

    /**
     * Resources requirements.
     */
    ResourcesConfig resources();

    /**
     * If set, the secret will mounted to the application container and its contents will be used for application
     * configuration.
     */
    Optional<String> appSecret();

    /**
     * If set, the config map will be mounted to the application container and its contents will be used for application
     * configuration.
     */
    Optional<String> appConfigMap();

    /**
     * RBAC configuration.
     */
    RbacConfig rbac();

    /**
     * If set, it will copy the security context configuration provided into the generated pod settings.
     */
    SecurityContextConfig securityContext();

    /**
     * Switch used to control whether non-idempotent fields are included in generated kubernetes resources to improve
     * git-ops compatibility.
     */
    @WithDefault("false")
    boolean idempotent();

    /**
     * VCS URI annotation configuration.
     */
    VCSUriConfig vcsUri();

    @Deprecated
    default Map<String, ContainerConfig> getSidecars() {
        return sidecars();
    }
}
