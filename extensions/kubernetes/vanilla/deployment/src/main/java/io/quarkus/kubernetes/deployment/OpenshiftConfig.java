
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_VERSION;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class OpenshiftConfig implements PlatformConfiguration {

    public static enum OpenshiftFlavor {
        v3,
        v4;
    }

    public static enum DeploymentResourceKind {
        Deployment,
        DeploymentConfig
    }

    /**
     * The OpenShift flavor / version to use.
     * Older versions of OpenShift have minor differrences in the labels and fields they support.
     * This option allows users to have their manifests automatically aligned to the OpenShift 'flavor' they use.
     */
    @ConfigItem(defaultValue = "v4")
    OpenshiftFlavor flavor;

    /**
     * The kind of the deployment resource to use.
     * Supported values are 'Deployment' and 'DeploymentConfig' defaulting to the later.
     */
    @ConfigItem(defaultValue = "DeploymentConfig")
    DeploymentResourceKind deploymentKind;

    /**
     * The name of the group this component belongs too
     */
    @ConfigItem
    Optional<String> partOf;

    /**
     * The name of the application. This value will be used for naming Kubernetes
     * resources like: - Deployment - Service and so on ...
     */
    @ConfigItem(defaultValue = "${quarkus.container-image.name}")
    Optional<String> name;

    /**
     * The version of the application.
     */
    @ConfigItem(defaultValue = "${quarkus.container-image.tag}")
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
     * Whether or not to add the build timestamp to the Kubernetes annotations
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
     * The host under which the application is going to be exposed
     * 
     * @deprecated Use the {@code quarkus.openshift.route.host} instead
     */
    @ConfigItem
    Optional<String> host;

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
     * If true, an Openshift Route will be created
     * 
     * @deprecated Use the {@code quarkus.openshift.route.exposition} instead
     */
    @ConfigItem
    boolean expose;

    /**
     * Openshift route configuration
     */
    ExpositionConfig route;

    /**
     * If true, the 'app.kubernetes.io/version' label will be part of the selectors of Service and DeploymentConfig
     */
    @ConfigItem(defaultValue = "true")
    boolean addVersionToLabelSelectors;

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

    public Optional<String> getHost() {
        return host;
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

    public ProbeConfig getLivenessProbe() {
        return livenessProbe;
    }

    public ProbeConfig getReadinessProbe() {
        return readinessProbe;
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
    public boolean isExpose() {
        return false;
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
     * If set, the config amp will mounted to the application container and its contents will be used for application
     * configuration.
     */
    @ConfigItem
    Optional<String> appConfigMap;

    public Optional<String> getAppSecret() {
        return this.appSecret;
    }

    public Optional<String> getAppConfigMap() {
        return this.appConfigMap;
    }

    @Override
    public Optional<ExpositionConfig> getExposition() {
        return Optional.of(route);
    }

    public String getDepoymentResourceGroup() {
        return deploymentKind == DeploymentResourceKind.Deployment ? DEPLOYMENT_GROUP : DEPLOYMENT_CONFIG_GROUP;
    }

    public String getDepoymentResourceVersion() {
        return deploymentKind == DeploymentResourceKind.Deployment ? DEPLOYMENT_VERSION : DEPLOYMENT_CONFIG_VERSION;
    }

    public String getDepoymentResourceKind() {
        return deploymentKind == DeploymentResourceKind.Deployment ? DEPLOYMENT : DEPLOYMENT_CONFIG;
    }
}
