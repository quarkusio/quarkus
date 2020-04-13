package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Environment variables to add to all containers
     */
    @ConfigItem
    Map<String, EnvConfig> envVars;

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
    Map<String, ContainerConfig> containers;

    /**
     * The target deployment platform.
     * Defaults to kubernetes. Can be kubernetes, openshift, knative etc, or any combination of the above as comma separated
     * list.
     */
    @ConfigItem(defaultValue = "kubernetes")
    List<String> deploymentTarget;

    /**
     * If true, a Kubernetes Ingress will be created
     */
    @ConfigItem(defaultValue = "false")
    boolean expose;

    public Optional<String> getPartOf() {
        return partOf;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getVersion() {
        return version;
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

    public Map<String, EnvConfig> getEnvVars() {
        return envVars;
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

    public Map<String, ContainerConfig> getContainers() {
        return containers;
    }

    @Override
    public boolean isExpose() {
        return expose;
    }
}
