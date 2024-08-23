package io.quarkus.kubernetes.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class KnativeConfig implements PlatformConfiguration {

    /**
     * The name of the group this component belongs too
     */
    @ConfigItem
    Optional<String> partOf;

    /**
     * The name of the application. This value will be used for naming Kubernetes
     * resources like: - Deployment - Service and so on ...
     */
    @ConfigItem
    Optional<String> name;

    /**
     * The version of the application.
     */
    @ConfigItem
    Optional<String> version;

    /**
     * The namespace the generated resources should belong to.
     * If not value is set, then the 'namespace' field will not be
     * added to the 'metadata' section of the generated manifests.
     * This in turn means that when the manifests are applied to a cluster,
     * the namespace will be resolved from the current Kubernetes context
     * (see https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/#context
     * for more details).
     */
    @ConfigItem
    Optional<String> namespace;

    /**
     * Custom labels to add to all resources
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    Map<String, String> labels;

    /**
     * Custom annotations to add to all resources
     */
    @ConfigItem
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations;

    /**
     * Whether to add the build timestamp to the Kubernetes annotations
     * This is a very useful way to have manifests of successive builds of the same
     * application differ - thus ensuring that Kubernetes will apply the updated resources
     */
    @ConfigItem(defaultValue = "true")
    boolean addBuildTimestamp;

    /**
     * Working directory
     */
    @ConfigItem
    Optional<String> workingDir;

    /**
     * The commands
     */
    @ConfigItem
    Optional<List<String>> command;

    /**
     * The arguments
     *
     * @return The arguments
     */
    @ConfigItem
    Optional<List<String>> arguments;

    /**
     * The service account
     */
    @ConfigItem
    Optional<String> serviceAccount;

    /**
     * The application ports
     */
    @ConfigItem
    Map<String, PortConfig> ports;

    /**
     * The type of service that will be generated for the application
     */
    @ConfigItem(defaultValue = "ClusterIP")
    ServiceType serviceType;

    /**
     * Image pull policy
     */
    @ConfigItem(defaultValue = "Always")
    ImagePullPolicy imagePullPolicy;

    /**
     * The image pull secret
     */
    @ConfigItem
    Optional<List<String>> imagePullSecrets;

    /**
     * Enable generation of image pull secret, when the container image username and
     * password are provided.
     */
    @ConfigItem(defaultValue = "false")
    boolean generateImagePullSecret;

    /**
     * The liveness probe
     */
    @ConfigItem
    ProbeConfig livenessProbe;

    /**
     * The readiness probe
     */
    @ConfigItem
    ProbeConfig readinessProbe;

    /**
     * The startup probe
     */
    @ConfigItem
    ProbeConfig startupProbe;

    /**
     * Prometheus configuration
     */
    @ConfigItem
    PrometheusConfig prometheus;

    /**
     * Volume mounts
     */
    @ConfigItem
    Map<String, MountConfig> mounts;

    /**
     * Secret volumes
     */
    @ConfigItem
    Map<String, SecretVolumeConfig> secretVolumes;

    /**
     * ConfigMap volumes
     */
    @ConfigItem
    Map<String, ConfigMapVolumeConfig> configMapVolumes;

    /**
     * EmptyDir volumes
     */
    @ConfigItem
    Optional<List<String>> emptyDirVolumes;

    /**
     * Git Repository volumes
     */
    @ConfigItem
    Map<String, GitRepoVolumeConfig> gitRepoVolumes;

    /**
     * Persistent Volume Claim volumes
     */
    @ConfigItem
    Map<String, PvcVolumeConfig> pvcVolumes;

    /**
     * AWS Elastic BlockStore volumes
     */
    @ConfigItem
    Map<String, AwsElasticBlockStoreVolumeConfig> awsElasticBlockStoreVolumes;

    /**
     * Azure file volumes
     */
    @ConfigItem
    Map<String, AzureFileVolumeConfig> azureFileVolumes;

    /**
     * Azure disk volumes
     */
    @ConfigItem
    Map<String, AzureDiskVolumeConfig> azureDiskVolumes;

    /**
     * If set, it will change the name of the container according to the configuration
     */
    @ConfigItem
    Optional<String> containerName;

    /**
     * Init containers
     */
    @ConfigItem
    Map<String, ContainerConfig> initContainers;

    /**
     * Sidecar containers
     */
    @ConfigItem
    Map<String, ContainerConfig> containers;

    /**
     * The host aliases
     */
    @ConfigItem
    Map<String, HostAliasConfig> hostAliases;

    /**
     * Resources requirements
     */
    @ConfigItem
    ResourcesConfig resources;

    /**
     * RBAC configuration
     */
    @ConfigItem
    RbacConfig rbac;

    /**
     * If true, the 'app.kubernetes.io/version' label will be part of the selectors of Service and Deployment
     */
    @ConfigItem(defaultValue = "true")
    boolean addVersionToLabelSelectors;

    /**
     * If true, the 'app.kubernetes.io/name' label will be part of the selectors of Service and Deployment
     */
    @ConfigItem(defaultValue = "true")
    boolean addNameToLabelSelectors;

    /**
     * Switch used to control whether non-idempotent fields are included in generated kubernetes resources to improve
     * git-ops compatibility
     */
    @ConfigItem(defaultValue = "false")
    boolean idempotent;

    /**
     * VCS URI annotation configuration.
     */
    @ConfigItem
    VCSUriConfig vcsUri;

    public Optional<String> getPartOf() {
        return partOf;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getVersion() {
        return version;
    }

    public Optional<String> getNamespace() {
        return namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean isAddBuildTimestamp() {
        return addBuildTimestamp;
    }

    @Override
    public boolean isAddNameToLabelSelectors() {
        return addNameToLabelSelectors;
    }

    @Override
    public boolean isAddVersionToLabelSelectors() {
        return addVersionToLabelSelectors;
    }

    @Override
    public String getTargetPlatformName() {
        return Constants.KNATIVE;
    }

    public Optional<String> getWorkingDir() {
        return workingDir;
    }

    public Optional<List<String>> getCommand() {
        return command;
    }

    public Optional<List<String>> getArguments() {
        return arguments;
    }

    public Optional<String> getServiceAccount() {
        return serviceAccount;
    }

    @Override
    public Optional<String> getContainerName() {
        return containerName;
    }

    public Map<String, PortConfig> getPorts() {
        return ports;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public ImagePullPolicy getImagePullPolicy() {
        return imagePullPolicy;
    }

    public Optional<List<String>> getImagePullSecrets() {
        return imagePullSecrets;
    }

    public boolean isGenerateImagePullSecret() {
        return generateImagePullSecret;
    }

    public ProbeConfig getLivenessProbe() {
        return livenessProbe;
    }

    public ProbeConfig getReadinessProbe() {
        return readinessProbe;
    }

    public ProbeConfig getStartupProbe() {
        return startupProbe;
    }

    public PrometheusConfig getPrometheusConfig() {
        return prometheus;
    }

    public Map<String, MountConfig> getMounts() {
        return mounts;
    }

    public Map<String, SecretVolumeConfig> getSecretVolumes() {
        return secretVolumes;
    }

    public Map<String, ConfigMapVolumeConfig> getConfigMapVolumes() {
        return configMapVolumes;
    }

    public List<String> getEmptyDirVolumes() {
        return emptyDirVolumes.orElse(Collections.emptyList());
    }

    public Map<String, GitRepoVolumeConfig> getGitRepoVolumes() {
        return gitRepoVolumes;
    }

    public Map<String, PvcVolumeConfig> getPvcVolumes() {
        return pvcVolumes;
    }

    public Map<String, AwsElasticBlockStoreVolumeConfig> getAwsElasticBlockStoreVolumes() {
        return awsElasticBlockStoreVolumes;
    }

    public Map<String, AzureFileVolumeConfig> getAzureFileVolumes() {
        return azureFileVolumes;
    }

    public Map<String, AzureDiskVolumeConfig> getAzureDiskVolumes() {
        return azureDiskVolumes;
    }

    public Map<String, ContainerConfig> getInitContainers() {
        return initContainers;
    }

    public Map<String, ContainerConfig> getSidecars() {
        return containers;
    }

    public Map<String, HostAliasConfig> getHostAliases() {
        return hostAliases;
    }

    public ResourcesConfig getResources() {
        return resources;
    }

    /**
     * Environment variables to add to all containers using the old syntax.
     *
     * @deprecated Use {@link #env} instead using the new syntax as follows:
     *             <ul>
     *             <li>{@code quarkus.kubernetes.env-vars.foo.field=fieldName} becomes
     *             {@code quarkus.kubernetes.env.fields.foo=fieldName}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.envvar.value=value} becomes
     *             {@code quarkus.kubernetes.env.vars.envvar=value}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.bar.configmap=configName} becomes
     *             {@code quarkus.kubernetes.env.configmaps=configName}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.baz.secret=secretName} becomes
     *             {@code quarkus.kubernetes.env.secrets=secretName}</li>
     *             </ul>
     */
    @ConfigItem
    @Deprecated
    Map<String, EnvConfig> envVars;

    /**
     * Environment variables to add to all containers.
     */
    @ConfigItem
    EnvVarsConfig env;

    @Deprecated
    public Map<String, EnvConfig> getEnvVars() {
        return envVars;
    }

    public EnvVarsConfig getEnv() {
        return env;
    }

    /**
     * Whether this service is cluster-local.
     * Cluster local services are not exposed to the outside world.
     * More information in <a href="https://knative.dev/docs/serving/services/private-services/">this link</a>.
     */
    @ConfigItem
    public boolean clusterLocal;

    /**
     * This value controls the minimum number of replicas each revision should have.
     * Knative will attempt to never have less than this number of replicas at any point in time.
     */
    @ConfigItem
    Optional<Integer> minScale;

    /**
     * This value controls the maximum number of replicas each revision should have.
     * Knative will attempt to never have more than this number of replicas running, or in the process of being created, at any
     * point in time.
     **/
    @ConfigItem
    Optional<Integer> maxScale;

    /**
     * The scale-to-zero values control whether Knative allows revisions to scale down to zero, or stops at “1”.
     */
    @ConfigItem(defaultValue = "true")
    boolean scaleToZeroEnabled;

    /**
     * Revision autoscaling configuration.
     */
    AutoScalingConfig revisionAutoScaling;

    /**
     * Global autoscaling configuration.
     */
    GlobalAutoScalingConfig globalAutoScaling;

    /**
     * The name of the revision.
     */
    @ConfigItem
    Optional<String> revisionName;

    /**
     * Traffic configuration.
     */
    @ConfigItem
    Map<String, TrafficConfig> traffic;

    /**
     * If set, the secret will mounted to the application container and its contents will be used for application configuration.
     */
    @ConfigItem
    Optional<String> appSecret;

    /**
     * If set, the config map will be mounted to the application container and its contents will be used for application
     * configuration.
     */
    @ConfigItem
    Optional<String> appConfigMap;

    /**
     * If set, it will copy the security context configuration provided into the generated pod settings.
     */
    @ConfigItem
    SecurityContextConfig securityContext;

    /**
     * If set to true, Quarkus will attempt to deploy the application to the target knative cluster
     */
    @ConfigItem(defaultValue = "false")
    boolean deploy;

    /**
     * If deploy is enabled, it will follow this strategy to update the resources to the target Knative cluster.
     */
    @ConfigItem(defaultValue = "CreateOrUpdate")
    DeployStrategy deployStrategy;

    public Optional<String> getAppSecret() {
        return this.appSecret;
    }

    public Optional<String> getAppConfigMap() {
        return this.appConfigMap;
    }

    @Override
    public SecurityContextConfig getSecurityContext() {
        return securityContext;
    }

    @Override
    public boolean isIdempotent() {
        return idempotent;
    }

    @Override
    public VCSUriConfig getVCSUri() {
        return vcsUri;
    }

    @Override
    public RbacConfig getRbacConfig() {
        return rbac;
    }
}
