
package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;

public interface PlatformConfiguration extends EnvVarHolder {

    Optional<String> getPartOf();

    Optional<String> getName();

    Optional<String> getVersion();

    Optional<String> getNamespace();

    Map<String, String> getLabels();

    Map<String, String> getAnnotations();

    boolean isAddBuildTimestamp();

    Optional<String> getWorkingDir();

    Optional<List<String>> getCommand();

    Optional<List<String>> getArguments();

    Optional<String> getServiceAccount();

    Optional<String> getHost();

    Map<String, PortConfig> getPorts();

    ServiceType getServiceType();

    ImagePullPolicy getImagePullPolicy();

    Optional<List<String>> getImagePullSecrets();

    ProbeConfig getLivenessProbe();

    ProbeConfig getReadinessProbe();

    PrometheusConfig getPrometheusConfig();

    Map<String, MountConfig> getMounts();

    Map<String, SecretVolumeConfig> getSecretVolumes();

    Map<String, ConfigMapVolumeConfig> getConfigMapVolumes();

    Map<String, GitRepoVolumeConfig> getGitRepoVolumes();

    Map<String, PvcVolumeConfig> getPvcVolumes();

    Map<String, AwsElasticBlockStoreVolumeConfig> getAwsElasticBlockStoreVolumes();

    Map<String, AzureFileVolumeConfig> getAzureFileVolumes();

    Map<String, AzureDiskVolumeConfig> getAzureDiskVolumes();

    Map<String, ContainerConfig> getInitContainers();

    Map<String, ContainerConfig> getSidecars();

    Map<String, HostAliasConfig> getHostAliases();

    ResourcesConfig getResources();

    default boolean isExpose() {
        return false;
    }

    default String getConfigName() {
        return getClass().getSimpleName().replaceAll("Config$", "").toLowerCase();
    }
}
