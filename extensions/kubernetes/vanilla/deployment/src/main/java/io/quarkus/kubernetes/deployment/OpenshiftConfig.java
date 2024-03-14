
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.S2I;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class OpenshiftConfig implements PlatformConfiguration {

    public static enum OpenshiftFlavor {
        v3,
        v4;
    }

    /**
     * The OpenShift flavor / version to use.
     * Older versions of OpenShift have minor differences in the labels and fields they support.
     * This option allows users to have their manifests automatically aligned to the OpenShift 'flavor' they use.
     */
    @ConfigItem(defaultValue = "v4")
    OpenshiftFlavor flavor;

    /**
     * The kind of the deployment resource to use.
     * Supported values are 'Deployment', 'StatefulSet', 'Job', 'CronJob' and 'DeploymentConfig'. Defaults to 'DeploymentConfig'
     * if {@code flavor == v3}, or 'Deployment' otherwise.
     * DeploymentConfig is deprecated as of OpenShift 4.14. See https://access.redhat.com/articles/7041372 for details.
     */
    @ConfigItem
    Optional<DeploymentResourceKind> deploymentKind;

    /**
     * The name of the group this component belongs too
     */
    @ConfigItem
    Optional<String> partOf;

    /**
     * The name of the application. This value will be used for naming Kubernetes
     * resources like: 'Deployment', 'Service' and so on...
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
    Map<String, String> labels;

    /**
     * Custom annotations to add to all resources
     */
    @ConfigItem
    Map<String, String> annotations;

    /**
     * Add the build timestamp to the Kubernetes annotations
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
     * The number of desired pods
     */
    @ConfigItem(defaultValue = "1")
    Integer replicas;

    /**
     * The type of service that will be generated for the application
     */
    @ConfigItem(defaultValue = "ClusterIP")
    ServiceType serviceType;

    /**
     * The nodePort to set when serviceType is set to nodePort
     */
    @ConfigItem
    OptionalInt nodePort;

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
     * Init containers
     */
    @ConfigItem
    Map<String, ContainerConfig> initContainers;

    /**
     * Sidecar containers
     *
     * @deprecated Use the {@code sidecars} property instead
     */
    @ConfigItem
    @Deprecated
    Map<String, ContainerConfig> containers;

    /**
     * Sidecar containers
     */
    @ConfigItem
    Map<String, ContainerConfig> sidecars;

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
     * If set, it will change the name of the container according to the configuration
     */
    @ConfigItem
    Optional<String> containerName;

    /**
     * Openshift route configuration
     */
    RouteConfig route;

    /**
     * If true, the 'app.kubernetes.io/version' label will be part of the selectors of Service and DeploymentConfig
     */
    @ConfigItem(defaultValue = "true")
    boolean addVersionToLabelSelectors;

    /**
     * If true, the 'app.kubernetes.io/name' label will be part of the selectors of Service and Deployment
     */
    @ConfigItem(defaultValue = "true")
    boolean addNameToLabelSelectors;

    /**
     * Job configuration. It's only used if and only if {@code quarkus.openshift.deployment-kind} is `Job`.
     */
    JobConfig job;

    /**
     * CronJob configuration. It's only used if and only if {@code quarkus.openshift.deployment-kind} is `CronJob`.
     */
    CronJobConfig cronJob;

    /**
     * RBAC configuration
     */
    RbacConfig rbac;

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

    public Integer getReplicas() {
        return replicas;
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

    public Map<String, HostAliasConfig> getHostAliases() {
        return hostAliases;
    }

    public ResourcesConfig getResources() {
        return resources;
    }

    public Map<String, ContainerConfig> getInitContainers() {
        return initContainers;
    }

    public Map<String, ContainerConfig> getSidecars() {
        if (!containers.isEmpty() && !sidecars.isEmpty()) {
            // done in order to make migration to the new property straight-forward
            throw new IllegalStateException(
                    "'quarkus.openshift.sidecars' and 'quarkus.openshift.containers' cannot be used together. Please use the former as the latter has been deprecated");
        }
        if (!containers.isEmpty()) {
            return containers;
        }

        return sidecars;
    }

    @Override
    public String getTargetPlatformName() {
        return Constants.OPENSHIFT;
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
     * If set, the secret will mounted to the application container and its contents will be used for application configuration.
     */
    @ConfigItem
    Optional<String> appSecret;

    /**
     * If set, the config amp will be mounted to the application container and its contents will be used for application
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
     * Debug configuration to be set in pods.
     */
    DebugConfig remoteDebug;

    /**
     * If set to true, Quarkus will attempt to deploy the application to the target Openshift cluster
     */
    @ConfigItem(defaultValue = "false")
    boolean deploy;

    /**
     * If deploy is enabled, it will follow this strategy to update the resources to the target OpenShift cluster.
     */
    @ConfigItem(defaultValue = "CreateOrUpdate")
    DeployStrategy deployStrategy;

    /**
     * Flag to enable init task externalization.
     * When enabled (default), all initialization tasks
     * created by extensions, will be externalized as Jobs.
     * In addition, the deployment will wait for these jobs.
     *
     * @Deprecated use {@link #initTasks} configuration instead
     */
    @Deprecated(since = "3.1", forRemoval = true)
    @ConfigItem(defaultValue = "true")
    boolean externalizeInit;

    /**
     * Init tasks configuration.
     *
     * The init tasks are automatically generated by extensions like Flyway to perform the database migration before staring
     * up the application.
     *
     * This property is only taken into account if `quarkus.openshift.externalize-init` is true.
     */
    @ConfigItem
    Map<String, InitTaskConfig> initTasks;

    /**
     * Default Init tasks configuration.
     *
     * The init tasks are automatically generated by extensions like Flyway to perform the database migration before staring
     * up the application.
     */
    @ConfigItem
    InitTaskConfig initTaskDefaults;

    /**
     * Switch used to control whether non-idempotent fields are included in generated kubernetes resources to improve
     * git-ops compatibility
     */
    @ConfigItem(defaultValue = "false")
    boolean idempotent;

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

    public DeployStrategy getDeployStrategy() {
        return deployStrategy;
    }

    @Override
    public RbacConfig getRbacConfig() {
        return rbac;
    }

    public static boolean isOpenshiftBuildEnabled(ContainerImageConfig containerImageConfig, Capabilities capabilities) {
        boolean implicitlyEnabled = ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities)
                .filter(c -> c.contains(OPENSHIFT) || c.contains(S2I)).isPresent();
        return containerImageConfig.builder.map(b -> b.equals(OPENSHIFT) || b.equals(S2I)).orElse(implicitlyEnabled);
    }

    public DeploymentResourceKind getDeploymentResourceKind(Capabilities capabilities) {
        if (deploymentKind.isPresent()) {
            return deploymentKind.filter(k -> k.isAvailalbleOn(OPENSHIFT)).get();
        } else if (capabilities.isPresent(Capability.PICOCLI)) {
            return DeploymentResourceKind.Job;
        }
        return (flavor == OpenshiftFlavor.v3) ? DeploymentResourceKind.DeploymentConfig : DeploymentResourceKind.Deployment;
    }
}
