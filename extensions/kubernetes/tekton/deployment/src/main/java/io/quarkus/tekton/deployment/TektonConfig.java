package io.quarkus.tekton.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.deployment.PvcVolumeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class TektonConfig {

    public static final String DEFAULT_DEPLOYER_IMAGE = "lachlanevenson/k8s-kubectl:v1.18.0";

    public static final String DEFAULT_JVM_DOCKERFILE = "src/main/docker/Dockerfile.jvm";
    public static final String DEFAULT_NATIVE_DOCKERFILE = "src/main/docker/Dockerfile.native";

    /**
     * Feature flag for tekton
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled;

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
     * The namespace the generated resources should belong to. If not value is set,
     * then the 'namespace' field will not be added to the 'metadata' section of the
     * generated manifests. This in turn means that when the manifests are applied
     * to a cluster, the namespace will be resolved from the current Kubernetes
     * context (see
     * https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/#context
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
     * The name of an external git pipeline resource.
     */
    @ConfigItem
    Optional<String> externalGitPipelineResource;

    /**
     * The name of the source workspace.
     */
    @ConfigItem(defaultValue = "source")
    Optional<String> sourceWorkspace;

    /**
     * The name of an external PVC to be used for the source workspace.
     */
    @ConfigItem
    Optional<String> externalSourceWorkspaceClaim;

    /**
     * The persistent volume claim configuration for the source workspace. The
     * option only makes sense when the PVC is going to be generated (no external
     * pvc specified).
     */
    @ConfigItem
    Optional<PvcVolumeConfig> sourceWorkspaceClaim;

    /**
     * The name of workspace to use as a maven artifact repository.
     */
    @ConfigItem
    Optional<String> m2Workspace;

    /**
     * The name of an external PVC to be used for the m2 artifact repository.
     */
    @ConfigItem
    Optional<String> externalM2WorkspaceClaim;

    /**
     * The persistent volume claim configuration for the artifact repository. The
     * option only makes sense when the PVC is going to be generated (no external
     * pvc specified).
     */
    @ConfigItem
    Optional<PvcVolumeConfig> m2WorkspaceClaim;

    /**
     * The builder image to use.
     */
    @ConfigItem
    Optional<String> builderImage;

    /**
     * The builder command to use.
     */
    @ConfigItem
    Optional<String> builderCommand;

    /**
     * The builder command arguments to use.
     */
    @ConfigItem
    Optional<List<String>> builderArguments;

    /**
     * The docker image to be used for the deployment task. Such image needs to have
     * kubectl available.
     */
    @ConfigItem(defaultValue = DEFAULT_DEPLOYER_IMAGE)
    Optional<String> deployerImage;

    /**
     * The service account to use for the image pushing tasks. An existing or a
     * generated service account can be used. If no existing service account is
     * provided one will be generated based on the context.
     */
    @ConfigItem
    Optional<String> imagePushServiceAccount;

    /**
     * The secret to use when generating an image push service account. When no
     * existing service account is provided, one will be generated. The generated
     * service account may or may not use an existing secret.
     */
    @ConfigItem
    Optional<String> imagePushSecret;

    /**
     * Wether to upload the local `.docker/config.json` to automatically create the
     * secret.
     */
    @ConfigItem
    boolean useLocalDockerConfigJson;

    /**
     * The username to use for generating image builder secrets.
     */
    @ConfigItem(defaultValue = "docker.io")
    Optional<String> registry;

    /**
     * The username to use for generating image builder secrets.
     */
    Optional<String> registryUsername;

    /**
     * The password to use for generating image builder secrets.
     */
    @ConfigItem
    Optional<String> registryPassword;

    /**
     * The default Dockerfile to use for jvm builds
     */
    @ConfigItem(defaultValue = DEFAULT_JVM_DOCKERFILE)
    public String jvmDockerfile;

    /**
     * The default Dockerfile to use for native builds
     */
    @ConfigItem(defaultValue = DEFAULT_NATIVE_DOCKERFILE)
    public String nativeDockerfile;
}
