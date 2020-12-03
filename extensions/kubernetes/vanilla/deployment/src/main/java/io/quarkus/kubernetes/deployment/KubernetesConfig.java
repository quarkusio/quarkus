package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class KubernetesConfig implements PlatformConfiguration {

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
     * The nodePort to set when serviceType is set to node-port.
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
     */
    @ConfigItem
    Map<String, ContainerConfig> sidecars;

    /**
     * The target deployment platform.
     * Defaults to kubernetes. Can be kubernetes, openshift, knative, minikube etc, or any combination of the above as comma
     * separated
     * list.
     */
    @ConfigItem
    Optional<List<String>> deploymentTarget;

    /**
     * The host aliases
     */
    @ConfigItem(name = "hostaliases")
    Map<String, HostAliasConfig> hostAliases;

    /**
     * Resources requirements
     */
    @ConfigItem
    ResourcesConfig resources;

    /**
     * If true, a Kubernetes Ingress will be created
     */
    @ConfigItem
    boolean expose;

    /**
     * If true, the 'app.kubernetes.io/version' label will be part of the selectors of Service and Deployment
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

    @Override
    public String getTargetPlatformName() {
        return Constants.KUBERNETES;
    }

    /**
     * Environment variables to add to all containers using the old syntax.
     *
     * @deprecated Use {@link #env} instead using the new syntax as follows:
     *             <ul>
     *             <li>{@code quarkus.kubernetes.env-vars.foo.field=fieldName} becomes
     *             {@code quarkus.kubernetes.env.fields.foo=fieldName}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.foo.value=value} becomes
     *             {@code quarkus.kubernetes.env.vars.foo=bar}</li>
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

    public Map<String, PortConfig> getPorts() {
        return ports;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public OptionalInt getNodePort() {
        return this.nodePort;
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

    public Map<String, ContainerConfig> getInitContainers() {
        return initContainers;
    }

    public Map<String, ContainerConfig> getSidecars() {
        return sidecars;
    }

    public Map<String, HostAliasConfig> getHostAliases() {
        return hostAliases;
    }

    public ResourcesConfig getResources() {
        return resources;
    }

    @Override
    public boolean isExpose() {
        return expose;
    }
}
