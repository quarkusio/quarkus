package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HostAlias;
import io.fabric8.kubernetes.api.model.HostAliasBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.SysctlBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecFluent;

/**
 * Base class for decorators handling the generation of deployment resources as identified by {@link DeploymentResourceKind},
 * optionally removing a specified default deployment resource that might have been added by default for a given manifest type,
 * e.g. {@link DeploymentResourceKind#DeploymentConfig} for an OpenShift manifest when we want to replace this default one by
 * another deployment type, such as a plain Deployment.
 *
 * @param <T> the type of deployment resource to create e.g. {@link io.fabric8.kubernetes.api.model.apps.Deployment}
 * @param <B> the {@link VisitableBuilder} type associated with the resource
 * @param <C> an optional configuration type that can be used to populate the resource's field, e.g. {@link JobConfig} or
 *        {@link Void} if such configuration is not needed
 */
abstract class BaseAddDeploymentResourceDecorator<T extends HasMetadata, B extends VisitableBuilder<T, B>, C extends PlatformConfiguration>
        extends BaseAddResourceDecorator<T, B, C> {
    private final Predicate<HasMetadata> toRemovePredicate;

    /**
     * Create a new decorator to handle deployment-like resources, optionally handling the removal of the default deployment
     * resource for the target platform.
     *
     * @param name the name of the resource to add
     * @param toAdd the type of deployment-like resource to add
     * @param config the optional configuration associated with the deployment resource, {@code null} otherwise
     * @param toRemove the optional resource type to remove or {@code null} if no such resource needs to be removed. Note that
     *        this will remove all (presumably only one) resources with the kind associated with this parameter and the
     *        specified name.
     */
    public BaseAddDeploymentResourceDecorator(String name, DeploymentResourceKind toAdd, C config,
            DeploymentResourceKind toRemove) {
        super(name, toAdd.getKind(), toAdd.getApiVersion(), config);
        if (toRemove != null && toRemove != toAdd) {
            toRemovePredicate = hm -> match(hm, toRemove.getApiVersion(), toRemove.getKind(), name);
        } else {
            toRemovePredicate = null;
        }
    }

    @Override
    protected void prepare(List<HasMetadata> items, KubernetesListBuilder list) {
        super.prepare(items, list);

        // remove other deployment kind if required
        if (toRemovePredicate != null) {
            for (HasMetadata item : items) {
                if (toRemovePredicate.test(item)) {
                    list.removeFromItems(item);
                    break;
                }
            }
        }
    }

    protected ReplicasAware replicasAwareOrNull() {
        final var config = config();
        if (config instanceof ReplicasAware replicasAware) {
            return replicasAware;
        } else {
            return null;
        }
    }

    protected int replicas(Integer initialValue, ReplicasAware config) {
        int replicas = initialValue == null ? 1 : initialValue;
        if (config != null) {
            final var fromConfig = config.replicas();
            replicas = fromConfig != null && fromConfig != 1 ? fromConfig : replicas;
        }
        return replicas;
    }

    protected <PS extends PodSpecFluent<?>> PS podSpecDefaults(PS podSpec) {
        // termination grace period seconds
        if (podSpec.getTerminationGracePeriodSeconds() == null) {
            podSpec.withTerminationGracePeriodSeconds(10L);
        }

        // add or edit container with deployment name
        final var name = name();
        final var container = createOrEditNamedContainer(podSpec, name);

        // configure limits and requests
        ContainerConverter.setLimitsAndRequests(config().resources(), container);

        // configure application container with security options if present
        final var securityContext = config().securityContext();
        final var maybeReadOnly = securityContext.readOnlyRootFilesystem();
        final var maybeEscalation = securityContext.allowPrivilegeEscalation();
        if (maybeEscalation.isPresent() || maybeReadOnly.isPresent()) {
            final var containerSecContext = container.editOrNewSecurityContext();
            maybeReadOnly.ifPresent(containerSecContext::withReadOnlyRootFilesystem);
            maybeEscalation.ifPresent(containerSecContext::withAllowPrivilegeEscalation);
            containerSecContext.endSecurityContext();
        }

        container.endContainer();

        configureMainApplicationPod(podSpec);

        podSpec.addAllToVolumes(configureVolumes());

        return podSpec;
    }

    protected <PS extends PodSpecFluent<?>> void configureMainApplicationPod(PS podSpec) {
        final var config = config();
        config.imagePullSecrets().ifPresent(l -> addImagePullSecrets(podSpec, l));

        config.hostAliases().entrySet().stream()
                .map(HostAliasConverter::toKubeHostAlias)
                .forEach(e -> addHostAlias(podSpec, e));

        config.nodeSelector().ifPresent(n -> addNodeSelector(podSpec, n.key(), n.value()));

        //        todo: deal with init and sidecar containers
        //        podSpec.addAllToInitContainers(
        //                config.initContainers().entrySet().stream().map(ContainerConverter::toKubeContainer).toList());
        //
        //        podSpec.addAllToContainers(config.sidecars().entrySet().stream().map(ContainerConverter::toKubeContainer).toList());

        if (config.securityContext().isAnyPropertySet()) {
            applySecuritySettings(podSpec, config.securityContext());
        }
    }

    private static void addImagePullSecrets(PodSpecFluent<?> podSpec, List<String> imagePullSecrets) {
        final var secrets = imagePullSecrets.stream()
                .filter(secret -> KubernetesCommonHelper.isNotNullOrEmpty(secret) &&
                        !podSpec.hasMatchingImagePullSecret(r -> secret.equals(r.getName())))
                .map(LocalObjectReference::new)
                .toList();
        podSpec.addAllToImagePullSecrets(secrets);
    }

    private static void addHostAlias(PodSpecFluent<?> podSpec, HostAlias alias) {
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

    private static void addNodeSelector(PodSpecFluent<?> podSpec, String selectorKey, String selectorValue) {
        podSpec.removeFromNodeSelector(selectorKey);
        podSpec.addToNodeSelector(selectorKey, selectorValue);
    }

    private static void applySecuritySettings(PodSpecFluent<?> podSpec, SecurityContextConfig securityContext) {
        podSpec.withSecurityContext(securityContext.toPodSecurityContext());

        PodSecurityContextBuilder securityContextBuilder = new PodSecurityContextBuilder();

        securityContext.runAsUser().ifPresent(securityContextBuilder::withRunAsUser);
        securityContext.runAsGroup().ifPresent(securityContextBuilder::withRunAsGroup);
        securityContext.runAsNonRoot().ifPresent(securityContextBuilder::withRunAsNonRoot);
        securityContext.supplementalGroups().ifPresent(securityContextBuilder::addAllToSupplementalGroups);
        securityContext.fsGroup().ifPresent(securityContextBuilder::withFsGroup);
        securityContext.sysctls().entrySet().stream()
                .map(entry -> new SysctlBuilder().withName(entry.getKey()).withValue(entry.getValue()).build())
                .forEach(securityContextBuilder::addToSysctls);
        securityContext.fsGroupChangePolicy().map(Enum::name).ifPresent(securityContextBuilder::withFsGroupChangePolicy);
        securityContext.buildSeLinuxOptions().ifPresent(securityContextBuilder::withSeLinuxOptions);
        securityContext.buildWindowsOptions().ifPresent(securityContextBuilder::withWindowsOptions);

        podSpec.withSecurityContext(securityContextBuilder.build());
    }

    protected void initFromConfig(JobSpecFluent<?> spec, JobConfig config) {
        spec.withCompletionMode(config.completionMode().name());
        spec.editTemplate().editSpec().withRestartPolicy(config.restartPolicy().name()).endSpec().endTemplate();
        config.parallelism().ifPresent(spec::withParallelism);
        config.completions().ifPresent(spec::withCompletions);
        config.backoffLimit().ifPresent(spec::withBackoffLimit);
        config.activeDeadlineSeconds().ifPresent(spec::withActiveDeadlineSeconds);
        config.ttlSecondsAfterFinished().ifPresent(spec::withTtlSecondsAfterFinished);
    }

    protected Collection<Volume> configureVolumes() {
        final var config = config();

        List<Volume> volumes = new ArrayList<>();
        config.secretVolumes().forEach((k, v) -> volumes.add(v.toVolume(k)));

        config.configMapVolumes().forEach((k, v) -> volumes.add(v.toVolume(k)));

        config.emptyDirVolumes().ifPresent(v -> v.forEach(
                e -> volumes.add(new VolumeBuilder().withName(e).withNewEmptyDir().endEmptyDir().build())));

        config.pvcVolumes().forEach((k, v) -> volumes.add(v.toVolume(k)));

        config.awsElasticBlockStoreVolumes().forEach((k, v) -> volumes.add(v.toVolume(k)));

        config.azureFileVolumes().forEach((k, v) -> volumes.add(v.toVolume(k)));

        config.azureDiskVolumes().forEach((k, v) -> volumes.add(v.toVolume(k)));
        return volumes;
    }
}
