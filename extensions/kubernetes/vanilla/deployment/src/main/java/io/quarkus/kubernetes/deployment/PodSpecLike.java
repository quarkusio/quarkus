package io.quarkus.kubernetes.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import io.fabric8.knative.serving.v1.RevisionSpecFluent;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.api.model.HostAlias;
import io.fabric8.kubernetes.api.model.HostAliasBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.Volume;

/**
 * Provides a unified interface over similar-but-different {@link io.fabric8.kubernetes.api.model.PodSpec} and
 * {@link io.fabric8.knative.serving.v1.RevisionSpec} so that we can have the same behavior shared by both.
 *
 * @param <C> the configuration type from which to initialize the specific spec
 * @param <CF> the {@link ContainerFluent} subtype so that we can identify and initialize the main application container
 */
interface PodSpecLike<C extends PlatformConfiguration, CF extends ContainerFluent<?>> {

    String SMALLRYE_CONFIG_LOCATIONS = "SMALLRYE_CONFIG_LOCATIONS";

    Long getTerminationGracePeriodSeconds();

    void withTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds);

    default void addImagePullSecrets(List<String> imagePullSecrets) {
        final var secrets = imagePullSecrets.stream()
                .filter(secret -> KubernetesCommonHelper.isNotNullOrEmpty(secret) &&
                        !hasMatchingImagePullSecret(r -> secret.equals(r.getName())))
                .map(LocalObjectReference::new)
                .toList();
        addAllToImagePullSecrets(secrets);
    }

    boolean hasMatchingImagePullSecret(Predicate<LocalObjectReferenceBuilder> imagePullSecret);

    void addAllToImagePullSecrets(List<LocalObjectReference> imagePullSecrets);

    void replaceNodeSelector(String selectorKey, String selectorValue);

    void addHostAlias(HostAlias hostAlias);

    void withSecurityContext(PodSecurityContext securityContext);

    void addAllToVolumes(Collection<Volume> volumes);

    CF createOrEditNamedContainer(String name);

    void endContainer();

    default void configure(String name, C config) {
        configure(name, config, null);
    }

    default void configure(String name, C config, Function<ContainerFluent<?>, Void> containerCustomizer) {
        // termination grace period seconds
        if (getTerminationGracePeriodSeconds() == null) {
            withTerminationGracePeriodSeconds(10L);
        }

        // add or edit container with deployment name
        final var container = createOrEditNamedContainer(name);

        // configure limits and requests
        config.resources().applyToContainer(container);

        // configure application container with security options if present
        final var securityContext = config.securityContext();
        final var maybeReadOnly = securityContext.readOnlyRootFilesystem();
        final var maybeEscalation = securityContext.allowPrivilegeEscalation();
        if (maybeEscalation.isPresent() || maybeReadOnly.isPresent()) {
            final var containerSecContext = container.editOrNewSecurityContext();
            maybeReadOnly.ifPresent(containerSecContext::withReadOnlyRootFilesystem);
            maybeEscalation.ifPresent(containerSecContext::withAllowPrivilegeEscalation);
            containerSecContext.endSecurityContext();
        }

        // volume mounts
        final Set<String> paths = new HashSet<>();
        config.appSecret().ifPresent(s -> {
            container.removeMatchingFromVolumeMounts(m -> m.getName().equals(s));
            container.addNewVolumeMount()
                    .withName(PlatformConfiguration.APP_SECRET)
                    .withMountPath(PlatformConfiguration.APP_SECRET_MOUNT_PATH)
                    .endVolumeMount();
            paths.add(PlatformConfiguration.APP_SECRET_MOUNT_PATH);
        });

        config.appConfigMap().ifPresent(s -> {
            container.removeMatchingFromVolumeMounts(m -> m.getName().equals(s));
            container.addNewVolumeMount()
                    .withName(PlatformConfiguration.APP_CONFIG_MAP)
                    .withMountPath(PlatformConfiguration.APP_CONFIG_MAP_MOUNT_PATH)
                    .endVolumeMount();
            paths.add(PlatformConfiguration.APP_CONFIG_MAP_MOUNT_PATH);
        });

        if (!paths.isEmpty()) {
            container.addNewEnv().withName(SMALLRYE_CONFIG_LOCATIONS).withValue(String.join(",", paths)).endEnv();
        }

        config.mounts().forEach((k, v) -> {
            container.removeMatchingFromVolumeMounts(m -> m.getName().equals(k));
            container.addToVolumeMounts(v.toVolumeMount(k));
        });

        if (containerCustomizer != null) {
            containerCustomizer.apply(container);
        }

        endContainer();

        configureMainApplicationPod(config);

        addAllToVolumes(config.toVolumes());
    }

    default void configureMainApplicationPod(C config) {
        config.imagePullSecrets().ifPresent(this::addImagePullSecrets);

        config.hostAliases().entrySet().stream()
                .map(HostAliasConverter::toKubeHostAlias)
                .forEach(this::addHostAlias);

        config.nodeSelector().ifPresent(n -> replaceNodeSelector(n.key(), n.value()));

        //        todo: deal with init and sidecar containers
        //        podSpec.addAllToInitContainers(
        //                config.initContainers().entrySet().stream().map(ContainerConverter::toKubeContainer).toList());
        //
        //        podSpec.addAllToContainers(config.sidecars().entrySet().stream().map(ContainerConverter::toKubeContainer).toList());

        final var securityContextConfig = config.securityContext();
        if (securityContextConfig.isAnyPropertySet()) {
            withSecurityContext(securityContextConfig.toPodSecurityContext());
        }
    }

    /**
     * Creates a {@link PodSpecLike} from the specified {@link io.fabric8.kubernetes.api.model.PodSpec}
     */
    static <C extends PlatformConfiguration> PodSpecLike<C, PodSpecFluent<?>.ContainersNested<?>> fromPodSpec(
            PodSpecFluent<?> podSpec) {
        return new PodSpecLike<>() {
            final ThreadLocal<PodSpecFluent<?>.ContainersNested<?>> container = new ThreadLocal<>();

            @Override
            public Long getTerminationGracePeriodSeconds() {
                return podSpec.getTerminationGracePeriodSeconds();
            }

            @Override
            public void withTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {
                podSpec.withTerminationGracePeriodSeconds(terminationGracePeriodSeconds);
            }

            @Override
            public boolean hasMatchingImagePullSecret(Predicate<LocalObjectReferenceBuilder> imagePullSecret) {
                return podSpec.hasMatchingImagePullSecret(imagePullSecret);
            }

            @Override
            public void addAllToImagePullSecrets(List<LocalObjectReference> imagePullSecrets) {
                podSpec.addAllToImagePullSecrets(imagePullSecrets);
            }

            @Override
            public void replaceNodeSelector(String selectorKey, String selectorValue) {
                podSpec.removeFromNodeSelector(selectorKey);
                podSpec.addToNodeSelector(selectorKey, selectorValue);
            }

            @Override
            public void addHostAlias(HostAlias alias) {
                Predicate<HostAliasBuilder> matchingHostAlias = host -> host.getIp().equals(alias.getIp());
                if (podSpec.hasMatchingHostAlias(matchingHostAlias)) {
                    // if we already have a host alias with that ip, add the aliases
                    podSpec.editMatchingHostAlias(matchingHostAlias)
                            .addAllToHostnames(alias.getHostnames())
                            .endHostAlias();
                } else {
                    // otherwise, add a new one
                    podSpec.addNewHostAlias().withIp(alias.getIp()).withHostnames(alias.getHostnames()).endHostAlias();
                }
            }

            @Override
            public void withSecurityContext(PodSecurityContext securityContext) {
                podSpec.withSecurityContext(securityContext);
            }

            @Override
            public void addAllToVolumes(Collection<Volume> volumes) {
                podSpec.addAllToVolumes(volumes);
            }

            @Override
            public PodSpecFluent<?>.ContainersNested<?> createOrEditNamedContainer(String name) {
                final var withName = (Predicate<ContainerBuilder>) cb -> cb.getName().equals(name);
                final PodSpecFluent<?>.ContainersNested<?> container;
                if (!podSpec.hasMatchingContainer(withName)) {
                    container = podSpec.addNewContainer().withName(name);
                } else {
                    container = podSpec.editMatchingContainer(withName);
                }
                this.container.set(container);
                return container;
            }

            @Override
            public void endContainer() {
                container.get().endContainer();
                container.remove();
            }
        };
    }

    /**
     * Creates a {@link PodSpecLike} from the specified {@link io.fabric8.knative.serving.v1.RevisionSpec}
     */
    static <C extends PlatformConfiguration> PodSpecLike<C, RevisionSpecFluent<?>.ContainersNested<?>> fromRevisionSpec(
            RevisionSpecFluent<?> revisionSpec) {
        return new PodSpecLike<>() {
            final ThreadLocal<RevisionSpecFluent<?>.ContainersNested<?>> container = new ThreadLocal<>();

            @Override
            public Long getTerminationGracePeriodSeconds() {
                return revisionSpec.getTerminationGracePeriodSeconds();
            }

            @Override
            public void withTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {
                revisionSpec.withTerminationGracePeriodSeconds(terminationGracePeriodSeconds);
            }

            @Override
            public boolean hasMatchingImagePullSecret(Predicate<LocalObjectReferenceBuilder> imagePullSecret) {
                return revisionSpec.hasMatchingImagePullSecret(imagePullSecret);
            }

            @Override
            public void addAllToImagePullSecrets(List<LocalObjectReference> imagePullSecrets) {
                revisionSpec.addAllToImagePullSecrets(imagePullSecrets);
            }

            @Override
            public void replaceNodeSelector(String selectorKey, String selectorValue) {
                revisionSpec.removeFromNodeSelector(selectorKey);
                revisionSpec.addToNodeSelector(selectorKey, selectorValue);
            }

            @Override
            public void addHostAlias(HostAlias alias) {
                Predicate<HostAlias> matchingHostAlias = host -> host.getIp().equals(alias.getIp());
                if (revisionSpec.hasMatchingHostAlias(matchingHostAlias)) {
                    // if we already have a host alias with that ip, add the aliases
                    revisionSpec.getMatchingHostAlias(matchingHostAlias).edit().addAllToHostnames(alias.getHostnames());
                } else {
                    // otherwise, add a new one
                    revisionSpec.withHostAliases(new HostAlias(alias.getHostnames(), alias.getIp()));
                }
            }

            @Override
            public void withSecurityContext(PodSecurityContext securityContext) {
                revisionSpec.withSecurityContext(securityContext);
            }

            @Override
            public void addAllToVolumes(Collection<Volume> volumes) {
                revisionSpec.addAllToVolumes(volumes);
            }

            @Override
            public RevisionSpecFluent<?>.ContainersNested<?> createOrEditNamedContainer(String name) {
                final var withName = (Predicate<ContainerBuilder>) cb -> cb.getName().equals(name);
                final RevisionSpecFluent<?>.ContainersNested<?> container;
                if (!revisionSpec.hasMatchingContainer(withName)) {
                    container = revisionSpec.addNewContainer().withName(name);
                } else {
                    container = revisionSpec.editMatchingContainer(withName);
                }
                this.container.set(container);
                return container;
            }

            @Override
            public void endContainer() {
                container.get().endContainer();
                container.remove();
            }
        };
    }
}
