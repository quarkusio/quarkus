package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAwsElasticBlockStoreVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureDiskVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureFileVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddEmptyDirVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddImagePullSecretDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddMountDecorator;
import io.dekorate.kubernetes.decorator.AddSecretVolumeDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.project.Project;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEffectiveServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthStartupPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesInitContainerBuildItem;
import io.quarkus.kubernetes.spi.KubernetesJobBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesNamespaceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesProbePortNameBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.Targetable;

public abstract class BaseKubeProcessor<P, C extends PlatformConfiguration> {

    protected abstract int priority();

    protected abstract String deploymentTarget();

    protected abstract P portConfigurator(Port port);

    protected abstract C config();

    protected abstract Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports);

    protected String clusterType() {
        return KUBERNETES;
    }

    protected boolean enabled() {
        return true;
    }

    protected DeploymentResourceKind deploymentResourceKind(Capabilities capabilities) {
        return DeploymentResourceKind.Deployment;
    }

    protected void produceDeploymentBuildItem(ApplicationInfoBuildItem applicationInfo,
            Capabilities capabilities,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        final var enabled = enabled();
        final var config = config();
        final var drk = deploymentResourceKind(capabilities);
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(deploymentTarget(), drk.getKind(), drk.getGroup(), drk.getVersion(),
                        priority(), enabled, config.deployStrategy()));

        if (enabled) {
            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(
                    new KubernetesResourceMetadataBuildItem(clusterType(), drk.getGroup(), drk.getVersion(), drk.getKind(),
                            name));
        }
    }

    protected void createAnnotations(BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config().annotations()
                .forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, deploymentTarget())));
    }

    protected void createLabels(BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config().labels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, deploymentTarget()));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
        labels.produce(
                new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", deploymentTarget()));
    }

    protected Stream<Port> asStream(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.combinePorts(ports, config()).values().stream();
    }

    protected List<ConfiguratorBuildItem> createConfigurators(List<KubernetesPortBuildItem> ports) {
        return asStream(ports)
                .map(port -> new ConfiguratorBuildItem(portConfigurator(port)))
                .collect(Collectors.toCollection(ArrayList::new)); // need a mutable list so sub-classes can add to it
    }

    protected KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(
            ApplicationInfoBuildItem applicationInfo,
            List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        final var config = config();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        return KubernetesCommonHelper.computeEffectiveServiceAccount(name, deploymentTarget(),
                config, serviceAccountsFromExtensions,
                decorators);
    }

    protected void createNamespace(BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        config().namespace().ifPresent(n -> namespace.produce(new KubernetesNamespaceBuildItem(deploymentTarget(), n)));
    }

    protected boolean isDeploymentTargetDisabled(List<KubernetesDeploymentTargetBuildItem> targets) {
        return targets.stream()
                .filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .noneMatch(t -> deploymentTarget().equals(t.getName()));
    }

    protected void externalizeInitTasks(
            ApplicationInfoBuildItem applicationInfo,
            ContainerImageInfoBuildItem image,
            List<InitTaskBuildItem> initTasks,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccount,
            BuildProducer<DecoratorBuildItem> decorators) {
        final var config = config();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        if (config.externalizeInit()) {
            InitTaskProcessor.process(deploymentTarget(), name, image, initTasks, config.initTaskDefaults(), config.initTasks(),
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }

    protected record DecoratorsContext(List<DecoratorBuildItem> decorators, String target, String name, boolean done) {

        static DecoratorsContext empty = new DecoratorsContext(List.of(), ANY, null, true);

        static DecoratorsContext newContext(String target, String name) {
            return new DecoratorsContext(new ArrayList<>(), target, name, false);
        }

        void add(Decorator<?> decorator) {
            decorators.add(new DecoratorBuildItem(target, decorator));
        }

        void addToAnyTarget(Decorator<?> decorator) {
            decorators.add(new DecoratorBuildItem(decorator));
        }

        void addAll(List<DecoratorBuildItem> others) {
            decorators.addAll(others);
        }

        boolean isValidTarget(String target) {
            return target == null || this.target.equals(target);
        }

        boolean isValidTarget(DecoratorBuildItem decorator) {
            return isValidTarget(decorator.getGroup());
        }

        boolean isValidTarget(Targetable targetable) {
            return targetable.isActiveFor(this.target, false);
        }

        <D extends Decorator<?>> List<D> decoratorsOfType(Class<D> clazz) {
            return decorators.stream()
                    .filter(this::isValidTarget)
                    .map(d -> d.getDecorator(clazz))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected DecoratorsContext commonDecorators(
            ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesNamespaceBuildItem> namespaces,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {
        if (isDeploymentTargetDisabled(targets)) {
            return DecoratorsContext.empty;
        }

        final var config = config();
        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        final var context = DecoratorsContext.newContext(deploymentTarget(), name);

        final var namespace = Targetable.filteredByTarget(namespaces, clusterType(), true).findFirst();

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = optionalPort(ports);

        context.addAll(KubernetesCommonHelper.createDecorators(project, context.target, context.name, namespace, config,
                metricsConfiguration, kubernetesClientConfiguration,
                annotations, labels, image, command,
                port, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                clusterRoleBindings));

        context.add(new ApplyImagePullPolicyDecorator(name, pullPolicy()));
        image.ifPresent(i -> context.add(new ApplyContainerImageDecorator(name, i.getImage())));

        var stream = Stream.concat(config.convertToBuildItems().stream(), Targetable.filteredByTarget(envs, clusterType()));
        if (config.idempotent()) {
            stream = stream.sorted(Comparator.comparing(e -> EnvConverter.convertName(e.getName())));
        }
        stream.map(e -> new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                .withName(EnvConverter.convertName(e.getName()))
                .withValue(e.getValue())
                .withSecret(e.getSecret())
                .withConfigmap(e.getConfigMap())
                .withField(e.getField())
                .withPrefix(e.getPrefix())
                .build()))
                .forEach(context::add);

        return context;
    }

    protected void initTasks(DecoratorsContext context, List<KubernetesInitContainerBuildItem> initContainers,
            List<KubernetesJobBuildItem> jobs) {
        createInitContainerDecorators(context, initContainers);
        createInitJobDecorators(context, jobs);
    }

    private void createInitContainerDecorators(DecoratorsContext context, List<KubernetesInitContainerBuildItem> items) {
        List<AddEnvVarDecorator> envVarDecorators = context.decoratorsOfType(AddEnvVarDecorator.class);
        List<AddMountDecorator> mountDecorators = context.decoratorsOfType(AddMountDecorator.class);

        items.stream()
                .filter(context::isValidTarget)
                .forEach(item -> {
                    io.dekorate.kubernetes.config.ContainerBuilder containerBuilder = new io.dekorate.kubernetes.config.ContainerBuilder()
                            .withName(item.getName())
                            .withImage(item.getImage())
                            .withImagePullPolicy(ImagePullPolicy.valueOf(item.getImagePullPolicy()))
                            .withCommand(item.getCommand().toArray(new String[0]))
                            .withArguments(item.getArguments().toArray(new String[0]));

                    if (item.isSharedEnvironment()) {
                        for (final AddEnvVarDecorator delegate : envVarDecorators) {
                            context.add(new ApplicationContainerDecorator<ContainerBuilder>(context.name, item.getName()) {
                                @Override
                                public void andThenVisit(ContainerBuilder builder) {
                                    delegate.andThenVisit(builder);
                                    // Currently, we have no way to filter out provided env vars.
                                    // So, we apply them on top of every change.
                                    // This needs to be addressed in dekorate to make things more efficient
                                    for (Map.Entry<String, String> e : item.getEnvVars().entrySet()) {
                                        builder.removeMatchingFromEnv(p -> p.getName().equals(e.getKey()));
                                        builder.addNewEnv()
                                                .withName(e.getKey())
                                                .withValue(e.getValue())
                                                .endEnv();

                                    }
                                }
                            });
                        }
                    }

                    if (item.isSharedFilesystem()) {
                        for (final AddMountDecorator delegate : mountDecorators) {
                            context.add(new ApplicationContainerDecorator<ContainerBuilder>(context.target, item.getName()) {
                                @Override
                                public void andThenVisit(ContainerBuilder builder) {
                                    delegate.andThenVisit(builder);
                                }
                            });
                        }
                    }

                    context.add(new AddInitContainerDecorator(context.name, containerBuilder
                            .addAllToEnvVars(item.getEnvVars().entrySet().stream().map(e -> new EnvBuilder()
                                    .withName(e.getKey())
                                    .withValue(e.getValue())
                                    .build()).collect(Collectors.toList()))
                            .build()));
                });
    }

    private void createInitJobDecorators(DecoratorsContext context, List<KubernetesJobBuildItem> items) {
        List<AddEnvVarDecorator> envVarDecorators = context.decoratorsOfType(AddEnvVarDecorator.class);

        List<NamedResourceDecorator<?>> volumeDecorators = context.decorators.stream()
                .filter(context::isValidTarget)
                .filter(d -> d.getDecorator() instanceof AddEmptyDirVolumeDecorator
                        || d.getDecorator() instanceof AddSecretVolumeDecorator
                        || d.getDecorator() instanceof AddAzureDiskVolumeDecorator
                        || d.getDecorator() instanceof AddAzureFileVolumeDecorator
                        || d.getDecorator() instanceof AddAwsElasticBlockStoreVolumeDecorator)
                .map(d -> (NamedResourceDecorator<?>) d.getDecorator())
                .collect(Collectors.toList());

        List<AddMountDecorator> mountDecorators = context.decoratorsOfType(AddMountDecorator.class);

        List<AddImagePullSecretDecorator> imagePullSecretDecorators = context
                .decoratorsOfType(AddImagePullSecretDecorator.class);

        List<ApplyServiceAccountNameDecorator> serviceAccountDecorators = context
                .decoratorsOfType(ApplyServiceAccountNameDecorator.class);

        Targetable.filteredByTarget(items, context.target).forEach(item -> {

            for (final AddImagePullSecretDecorator delegate : imagePullSecretDecorators) {
                context.add(new NamedResourceDecorator<PodSpecBuilder>("Job", item.getName()) {
                    @Override
                    public void andThenVisit(PodSpecBuilder builder, ObjectMeta meta) {
                        delegate.andThenVisit(builder, meta);
                    }
                });
            }

            for (final ApplyServiceAccountNameDecorator delegate : serviceAccountDecorators) {
                context.add(new NamedResourceDecorator<PodSpecBuilder>("Job", item.getName()) {
                    @Override
                    public void andThenVisit(PodSpecBuilder builder, ObjectMeta meta) {
                        delegate.andThenVisit(builder, meta);
                    }
                });
            }

            context.add(new NamedResourceDecorator<ContainerBuilder>("Job", item.getName()) {
                @Override
                public void andThenVisit(ContainerBuilder builder, ObjectMeta meta) {
                    for (Map.Entry<String, String> e : item.getEnvVars().entrySet()) {
                        builder.removeMatchingFromEnv(p -> p.getName().equals(e.getKey()));
                        builder.addNewEnv()
                                .withName(e.getKey())
                                .withValue(e.getValue())
                                .endEnv();
                    }
                }
            });

            if (item.isSharedEnvironment()) {
                for (final AddEnvVarDecorator delegate : envVarDecorators) {
                    context.add(new NamedResourceDecorator<ContainerBuilder>("Job", item.getName()) {
                        @Override
                        public void andThenVisit(ContainerBuilder builder, ObjectMeta meta) {
                            delegate.andThenVisit(builder);
                            // Currently, we have no way to filter out provided env vars.
                            // So, we apply them on top of every change.
                            // This needs to be addressed in dekorate to make things more efficient
                            for (Map.Entry<String, String> e : item.getEnvVars().entrySet()) {
                                builder.removeMatchingFromEnv(p -> p.getName().equals(e.getKey()));
                                builder.addNewEnv()
                                        .withName(e.getKey())
                                        .withValue(e.getValue())
                                        .endEnv();

                            }
                        }
                    });
                }
            }

            if (item.isSharedFilesystem()) {
                for (final NamedResourceDecorator<?> delegate : volumeDecorators) {
                    context.add(new NamedResourceDecorator<PodSpecBuilder>("Job", item.getName()) {
                        @Override
                        public void andThenVisit(PodSpecBuilder builder, ObjectMeta meta) {
                            delegate.visit(builder);
                        }
                    });
                }

                for (final AddMountDecorator delegate : mountDecorators) {
                    context.add(new NamedResourceDecorator<ContainerBuilder>("Job", item.getName()) {
                        @Override
                        public void andThenVisit(ContainerBuilder builder, ObjectMeta meta) {
                            delegate.andThenVisit(builder);
                        }
                    });
                }

            }

            context.add(new CreateJobResourceFromImageDecorator(item.getName(), item.getImage(), item.getCommand(),
                    item.getArguments()));
        });
    }

    protected void probes(DecoratorsContext context, List<KubernetesPortBuildItem> ports,
            Optional<KubernetesProbePortNameBuildItem> portName) {
        final var config = config();
        addProbeHttpPortDecorator(context, LIVENESS_PROBE, config().livenessProbe(), portName, ports, config.ports());
        addProbeHttpPortDecorator(context, READINESS_PROBE, config.readinessProbe(), portName, ports, config.ports());
        addProbeHttpPortDecorator(context, STARTUP_PROBE, config.startupProbe(), portName, ports, config.ports());
    }

    /**
     * Create a decorator that sets the port to the http probe.
     * The rules for setting the probe are the following:
     * 1. if 'http-action-port' is set, use that.
     * 2. if 'http-action-port-name' is set, use that to lookup the port value.
     * 3. if a `KubernetesPorbePortBuild` is set, then use that to lookup the port.
     * 4. if we still haven't found a port fallback to 8080.
     *
     * @param probeKind The probe kind (e.g. readinessProbe, livenessProbe etc)
     * @param portName the probe port name build item
     * @param ports a list of kubernetes port build items
     */
    private void addProbeHttpPortDecorator(DecoratorsContext context, String probeKind,
            ProbeConfig probeConfig,
            Optional<KubernetesProbePortNameBuildItem> portName,
            List<KubernetesPortBuildItem> ports,
            Map<String, PortConfig> portsFromConfig) {

        //1. check if `httpActionPort` is defined
        //2. lookup port by `httpPortName`
        //3. fallback to DEFAULT_HTTP_PORT
        String httpPortName = probeConfig.httpActionPortName()
                .or(() -> portName.map(KubernetesProbePortNameBuildItem::getName))
                .orElse(HTTP_PORT);

        int port;
        PortConfig portFromConfig = portsFromConfig.get(httpPortName);
        if (probeConfig.httpActionPort().isPresent()) {
            port = probeConfig.httpActionPort().get();
        } else if (portFromConfig != null && portFromConfig.containerPort().isPresent()) {
            port = portFromConfig.containerPort().getAsInt();
        } else {
            port = ports.stream().filter(p -> httpPortName.equals(p.getName()))
                    .map(KubernetesPortBuildItem::getPort).findFirst().orElse(DEFAULT_HTTP_PORT);
        }

        // Resolve scheme property from:
        String scheme;
        if (probeConfig.httpActionScheme().isPresent()) {
            // 1. User in Probe config
            scheme = probeConfig.httpActionScheme().get();
        } else if (portFromConfig != null && portFromConfig.tls()) {
            // 2. User in Ports config
            scheme = SCHEME_HTTPS;
        } else if (portName.isPresent()
                && portName.get().getScheme() != null
                && httpPortName.equals(portName.get().getName())) {
            // 3. Extensions
            scheme = portName.get().getScheme();
        } else {
            // 4. Using the port number.
            scheme = port == 443 || port == 8443 ? SCHEME_HTTPS : SCHEME_HTTP;
        }

        // Applying to all deployments to mimic the same logic as the rest of probes in the method createProbeDecorators.
        context.add(new ApplyHttpGetActionPortDecorator(ANY, context.name, httpPortName, port, probeKind, scheme));
    }

    private static final String SCHEME_HTTP = "HTTP";
    private static final String SCHEME_HTTPS = "HTTPS";
    private static final String ANY = null;

    protected ImagePullPolicy pullPolicy() {
        return config().imagePullPolicy();
    }
}
