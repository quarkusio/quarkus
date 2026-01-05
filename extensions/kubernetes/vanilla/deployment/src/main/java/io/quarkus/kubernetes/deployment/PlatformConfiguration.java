package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface PlatformConfiguration extends EnvVarHolder {
    /**
     * The kind of the deployment resource to use.
     * Supported values are 'StatefulSet', 'Job', 'CronJob' and 'Deployment' defaulting to the latter.
     */
    Optional<DeploymentResourceKind> deploymentKind();

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

    /**
     * If deploy is enabled, it will follow this strategy to update the resources to the target Kubernetes cluster.
     */
    @WithDefault("CreateOrUpdate")
    DeployStrategy deployStrategy();

    /**
     * Init tasks configuration.
     * <p>
     * The init tasks are automatically generated by extensions like Flyway to perform the database migration before starting
     * up the application.
     * <p>
     * This property is only taken into account if `quarkus.openshift.externalize-init` is true.
     */
    @ConfigDocMapKey("task-name")
    Map<String, InitTaskConfig> initTasks();

    /**
     * Default init tasks configuration.
     * <p>
     * The init tasks are automatically generated by extensions like Flyway to perform the database migration before staring
     * up the application.
     */
    InitTaskConfig initTaskDefaults();

    /**
     * Job configuration. It's only used if and only if {@code quarkus.kubernetes.deployment-kind} is `Job`.
     */
    JobConfig job();

    /**
     * CronJob configuration. It's only used if and only if {@code quarkus.kubernetes.deployment-kind} is `CronJob`.
     */
    CronJobConfig cronJob();

    /**
     * Debug configuration to be set in pods.
     */
    DebugConfig remoteDebug();

    @ConfigDocIgnore
    default DeploymentResourceKind getDeploymentResourceKind(Capabilities capabilities) {
        if (deploymentKind().isPresent()) {
            return deploymentKind().filter(k -> k.isAvailalbleOn(KUBERNETES)).get();
        } else if (capabilities.isPresent(Capability.PICOCLI)) {
            return DeploymentResourceKind.Job;
        }
        return DeploymentResourceKind.Deployment;
    }
}
