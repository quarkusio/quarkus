package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.parseVCSUri;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.config.Annotation;
import io.dekorate.kubernetes.config.ConfigMapVolumeBuilder;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.MountBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.SecretVolumeBuilder;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddAwsElasticBlockStoreVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureDiskVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureFileVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddDockerConfigJsonSecretDecorator;
import io.dekorate.kubernetes.decorator.AddEmptyDirVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddHostAliasesDecorator;
import io.dekorate.kubernetes.decorator.AddImagePullSecretDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddMetadataToTemplateDecorator;
import io.dekorate.kubernetes.decorator.AddMountDecorator;
import io.dekorate.kubernetes.decorator.AddPvcVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSecretVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddSelectorToDeploymentSpecDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.AddStartupProbeDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyArgsDecorator;
import io.dekorate.kubernetes.decorator.ApplyCommandDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyLimitsCpuDecorator;
import io.dekorate.kubernetes.decorator.ApplyLimitsMemoryDecorator;
import io.dekorate.kubernetes.decorator.ApplyRequestsCpuDecorator;
import io.dekorate.kubernetes.decorator.ApplyRequestsMemoryDecorator;
import io.dekorate.kubernetes.decorator.ApplyWorkingDirDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.RemoveAnnotationDecorator;
import io.dekorate.kubernetes.decorator.RemoveFromMatchingLabelsDecorator;
import io.dekorate.kubernetes.decorator.RemoveFromSelectorDecorator;
import io.dekorate.kubernetes.decorator.RemoveLabelDecorator;
import io.dekorate.project.Project;
import io.dekorate.project.ScmInfo;
import io.dekorate.utils.Annotations;
import io.dekorate.utils.Labels;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.quarkus.builder.Version;
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
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;
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

    /**
     *
     * @param decorators The ongoing list of {@link DecoratorBuildItem} being populated
     * @param target The deployment target (e.g. kubernetes, openshift, knative)
     * @param name The name of the resource to accept the configuration param namespace
     * @param done Whether the processing of decorators is done or not
     */
    protected record DecoratorsContext(List<DecoratorBuildItem> decorators, String target, String name, boolean done) {

        static DecoratorsContext empty = new DecoratorsContext(List.of(), ANY, null, true);

        static DecoratorsContext newContext(String target, String name) {
            return new DecoratorsContext(new ArrayList<>(), target, name, false);
        }

        void addToTarget(Decorator<?> decorator, String target) {
            decorators.add(new DecoratorBuildItem(target, decorator));
        }

        void add(Decorator<?> decorator) {
            addToTarget(decorator, target);
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

    /**
     * Creates the common decorator build items.
     */
    private void createDecorators(DecoratorsContext context, Optional<Project> project,
            Optional<KubernetesNamespaceBuildItem> namespace,
            C config,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            Optional<Port> port,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessProbePath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessProbePath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings) {
        createLabelDecorators(context, config, labels);
        createAnnotationDecorators(context, project, config, metricsConfiguration, annotations, port);
        createPodDecorators(context, config);
        createContainerDecorators(context, namespace, config);
        createMountAndVolumeDecorators(context, config);
        createAppConfigVolumeAndEnvDecorators(context, config);
        createCommandDecorator(context, config, command);
        createArgsDecorator(context, config, command);

        // Handle Pull Secrets
        if (config.generateImagePullSecret()) {
            image.ifPresent(i -> i.getRegistry().ifPresent(registry -> {
                if (i.getUsername().isPresent() && i.getPassword().isPresent()) {
                    String imagePullSecret = context.name + "-pull-secret";
                    context.add(new AddImagePullSecretDecorator(context.name, imagePullSecret));
                    context.add(new AddDockerConfigJsonSecretDecorator(imagePullSecret,
                            registry, i.username.get(), i.password.get()));
                }
            }));
        }

        // Handle Probes
        if (port.isPresent()) {
            createProbeDecorators(context, config.livenessProbe(), config.readinessProbe(), config.startupProbe(),
                    livenessProbePath, readinessProbePath, startupPath);
        }

        // Handle RBAC
        createRbacDecorators(context, config, kubernetesClientConfiguration, roles, clusterRoles,
                serviceAccounts, roleBindings, clusterRoleBindings);

    }

    private static final String DEFAULT_ROLE_NAME_VIEW = "view";

    private void createRbacDecorators(DecoratorsContext context, C config,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesRoleBuildItem> rolesFromExtensions,
            List<KubernetesClusterRoleBuildItem> clusterRolesFromExtensions,
            List<KubernetesEffectiveServiceAccountBuildItem> effectiveServiceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindingsFromExtensions,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindingsFromExtensions) {
        boolean kubernetesClientRequiresRbacGeneration = kubernetesClientConfiguration
                .map(KubernetesClientCapabilityBuildItem::isGenerateRbac).orElse(false);
        Set<String> roles = new HashSet<>();
        Set<String> clusterRoles = new HashSet<>();
        final var name = context.name;
        final var target = context.target;

        // Add roles from configuration
        for (Map.Entry<String, RbacConfig.RoleConfig> roleFromConfig : config.rbac().roles().entrySet()) {
            RbacConfig.RoleConfig role = roleFromConfig.getValue();
            String roleName = role.name().orElse(roleFromConfig.getKey());
            context.add(new AddRoleResourceDecorator(name,
                    roleName,
                    role.namespace().orElse(null),
                    role.labels(),
                    toPolicyRulesList(role.policyRules())));

            roles.add(roleName);
        }

        // Add roles from extensions
        Targetable.filteredByTarget(rolesFromExtensions, target)
                .map(role -> new AddRoleResourceDecorator(name,
                        role.getName(),
                        role.getNamespace(),
                        Collections.emptyMap(),
                        role.getRules().stream().map(RBACUtil::from).toList()))
                .forEach(context::add);

        // Add cluster roles from configuration
        for (Map.Entry<String, RbacConfig.ClusterRoleConfig> clusterRoleFromConfig : config.rbac().clusterRoles().entrySet()) {
            RbacConfig.ClusterRoleConfig clusterRole = clusterRoleFromConfig.getValue();
            String clusterRoleName = clusterRole.name().orElse(clusterRoleFromConfig.getKey());
            context.add(new AddClusterRoleResourceDecorator(name,
                    clusterRoleName,
                    clusterRole.labels(),
                    toPolicyRulesList(clusterRole.policyRules())));
            clusterRoles.add(clusterRoleName);
        }

        // Add cluster roles from extensions
        Targetable.filteredByTarget(clusterRolesFromExtensions, target)
                .map(role -> new AddClusterRoleResourceDecorator(name,
                        role.getName(),
                        Collections.emptyMap(),
                        role.getRules().stream()
                                .map(RBACUtil::from)
                                .toList()))
                .forEach(context::add);

        // Retrieve SA for current target
        final var potentialSAs = Targetable.filteredByTarget(effectiveServiceAccounts, target).toList();
        if (potentialSAs.isEmpty()) {
            throw new RuntimeException("No effective service account found for application " + name);
        }
        if (potentialSAs.size() > 1) {
            throw new RuntimeException("More than one effective service account found for application " + name);
        }
        final var effectiveServiceAccount = potentialSAs.get(0);
        final var effectiveServiceAccountNamespace = effectiveServiceAccount.getNamespace();
        final var effectiveServiceAccountName = effectiveServiceAccount.getServiceAccountName();

        // Prepare default configuration
        String defaultRoleName = null;
        boolean defaultClusterWide = false;
        boolean requiresServiceAccount = false;
        if (!roles.isEmpty()) {
            // generate a role binding using this first role.
            defaultRoleName = roles.iterator().next();
        } else if (!clusterRoles.isEmpty()) {
            // generate a role binding using this first cluster role.
            defaultClusterWide = true;
            defaultRoleName = clusterRoles.iterator().next();
        }

        // Add role bindings from extensions
        Targetable.filteredByTarget(roleBindingsFromExtensions, target)
                .map(rb -> new AddRoleBindingResourceDecorator(name,
                        Strings.isNotNullOrEmpty(rb.getName()) ? rb.getName() : name + "-" + rb.getRoleRef().getName(),
                        rb.getNamespace(),
                        rb.getLabels(),
                        rb.getRoleRef(),
                        rb.getSubjects()))
                .forEach(context::add);

        // Add role bindings from configuration
        for (Map.Entry<String, RbacConfig.RoleBindingConfig> rb : config.rbac().roleBindings().entrySet()) {
            String rbName = rb.getValue().name().orElse(rb.getKey());
            RbacConfig.RoleBindingConfig roleBinding = rb.getValue();

            List<Subject> subjects = new ArrayList<>();
            if (roleBinding.subjects().isEmpty()) {
                requiresServiceAccount = true;
                subjects.add(new Subject(null, SERVICE_ACCOUNT,
                        effectiveServiceAccountName,
                        effectiveServiceAccountNamespace));
            } else {
                for (Map.Entry<String, RbacConfig.SubjectConfig> s : roleBinding.subjects().entrySet()) {
                    String subjectName = s.getValue().name().orElse(s.getKey());
                    RbacConfig.SubjectConfig subject = s.getValue();
                    subjects.add(new Subject(subject.apiGroup().orElse(null),
                            subject.kind(),
                            subjectName,
                            subject.namespace().orElse(null)));
                }
            }

            String roleName = roleBinding.roleName().orElse(defaultRoleName);
            if (roleName == null) {
                throw new IllegalStateException("No role has been set in the RoleBinding resource!");
            }

            boolean clusterWide = roleBinding.clusterWide().orElse(defaultClusterWide);
            context.add(new AddRoleBindingResourceDecorator(name,
                    rbName,
                    null, // todo: should namespace be providable via config?
                    roleBinding.labels(),
                    new RoleRef(roleName, clusterWide),
                    subjects.toArray(new Subject[0])));
        }

        // Add cluster role bindings from extensions
        Targetable.filteredByTarget(clusterRoleBindingsFromExtensions, target)
                .map(rb -> new AddClusterRoleBindingResourceDecorator(name,
                        Strings.isNotNullOrEmpty(rb.getName()) ? rb.getName() : name + "-" + rb.getRoleRef().getName(),
                        rb.getLabels(),
                        rb.getRoleRef(),
                        rb.getSubjects()))
                .forEach(context::add);

        // Add cluster role bindings from configuration
        for (Map.Entry<String, RbacConfig.ClusterRoleBindingConfig> rb : config.rbac().clusterRoleBindings().entrySet()) {
            String rbName = rb.getValue().name().orElse(rb.getKey());
            RbacConfig.ClusterRoleBindingConfig clusterRoleBinding = rb.getValue();

            List<Subject> subjects = new ArrayList<>();
            if (clusterRoleBinding.subjects().isEmpty()) {
                throw new IllegalStateException("No subjects have been set in the ClusterRoleBinding resource!");
            }

            for (Map.Entry<String, RbacConfig.SubjectConfig> s : clusterRoleBinding.subjects().entrySet()) {
                String subjectName = s.getValue().name().orElse(s.getKey());
                RbacConfig.SubjectConfig subject = s.getValue();
                subjects.add(new Subject(subject.apiGroup().orElse(null),
                        subject.kind(),
                        subjectName,
                        subject.namespace().orElse(null)));
            }

            context.add(new AddClusterRoleBindingResourceDecorator(name,
                    rbName,
                    clusterRoleBinding.labels(),
                    new RoleRef(clusterRoleBinding.roleName(), true),
                    subjects.toArray(new Subject[0])));
        }

        // if no role bindings were created, then automatically create one if:
        if (config.rbac().roleBindings().isEmpty()) {
            if (defaultRoleName != null) {
                // generate a default role binding if a default role name was configured
                requiresServiceAccount = true;
                context.add(new AddRoleBindingResourceDecorator(name,
                        name,
                        null, // todo: should namespace be providable via config?
                        Collections.emptyMap(),
                        new RoleRef(defaultRoleName, defaultClusterWide),
                        new Subject(null, SERVICE_ACCOUNT,
                                effectiveServiceAccountName,
                                effectiveServiceAccountNamespace)));
            } else if (kubernetesClientRequiresRbacGeneration) {
                // the property `quarkus.kubernetes-client.generate-rbac` is enabled
                // and the kubernetes-client extension is present
                requiresServiceAccount = true;
                context.add(new AddRoleBindingResourceDecorator(name,
                        name + "-" + DEFAULT_ROLE_NAME_VIEW,
                        null, // todo: should namespace be providable via config?
                        Collections.emptyMap(),
                        new RoleRef(DEFAULT_ROLE_NAME_VIEW, true),
                        new Subject(null, SERVICE_ACCOUNT,
                                effectiveServiceAccountName,
                                effectiveServiceAccountNamespace)));
            }
        }

        // generate service account if none is set, and it's required by other resources
        if (requiresServiceAccount) {
            // and generate the resource
            context.add(new AddServiceAccountResourceDecorator(name, effectiveServiceAccountName,
                    effectiveServiceAccountNamespace,
                    Collections.emptyMap()));
        }

        // set service account in deployment resource if the user sets a service account,
        // or it's required for a dependant resource.
        if (effectiveServiceAccount.wasSet() || requiresServiceAccount) {
            context.add(new ApplyServiceAccountNameDecorator(name, effectiveServiceAccountName));
        }
    }

    private void createLabelDecorators(DecoratorsContext context, C config, List<KubernetesLabelBuildItem> labels) {
        context.add(new AddMetadataToTemplateDecorator());
        context.add(new AddSelectorToDeploymentSpecDecorator());
        final var name = context.name();

        labels.forEach(l -> context.addToTarget(new AddLabelDecorator(name, l.getKey(), l.getValue()), l.getTarget()));

        if (!config.addVersionToLabelSelectors() || config.idempotent()) {
            context.add(new RemoveFromSelectorDecorator(name, Labels.VERSION));
            context.add(new RemoveFromMatchingLabelsDecorator(name, Labels.VERSION));
        }

        if (config.idempotent()) {
            context.add(new RemoveLabelDecorator(name, Labels.VERSION));
        }

        if (!config.addNameToLabelSelectors()) {
            context.add(new RemoveLabelDecorator(name, Labels.NAME));
            context.add(new RemoveFromSelectorDecorator(name, Labels.NAME));
            context.add(new RemoveFromMatchingLabelsDecorator(name, Labels.NAME));
        }
    }

    /**
     * If user defines a custom command via configuration, this is used.
     * If not, it will use the one from other extensions.
     *
     * @param config The {@link PlatformConfiguration} instance
     * @param command Optional command item from other extensions
     */
    private void createCommandDecorator(DecoratorsContext context, C config, Optional<KubernetesCommandBuildItem> command) {
        // If not, we use the command that has been provided in other extensions (if any).
        if (config.command().isPresent()) {
            // If command has been set in configuration, we use it
            context.add(new ApplyCommandDecorator(context.name, config.command().get().toArray(new String[0])));
        } else {
            command.ifPresent(kubeCmdBI -> context
                    .add(new ApplyCommandDecorator(context.name, kubeCmdBI.getCommand().toArray(new String[0]))));
        }
    }

    /**
     * If user defines arguments via configuration, then these will be merged to the ones from other extensions.
     * If not, then only the arguments from other extensions will be used if any.
     *
     * @param config The {@link PlatformConfiguration} instance
     * @param command Optional command item from other extensions
     */
    private void createArgsDecorator(DecoratorsContext context, C config, Optional<KubernetesCommandBuildItem> command) {
        List<String> args = new ArrayList<>();
        command.ifPresent(cmd -> args.addAll(cmd.getArgs()));
        config.arguments().ifPresent(args::addAll);

        if (!args.isEmpty()) {
            context.add(new ApplyArgsDecorator(context.name, args.toArray(new String[0])));
        }
    }

    /**
     * Creates container decorator build items.
     *
     * @param config The {@link PlatformConfiguration} instance
     */
    private void createContainerDecorators(DecoratorsContext context,
            Optional<KubernetesNamespaceBuildItem> namespace,
            C config) {

        namespace.ifPresent(n -> {
            context.add(new AddNamespaceDecorator(n.getNamespace()));
            context.add(new AddNamespaceToSubjectDecorator(n.getNamespace()));
        });

        config.workingDir().ifPresent(w -> context.add(new ApplyWorkingDirDecorator(context.name, w)));
    }

    /**
     * Creates pod decorator build items.
     *
     * @param config The {@link PlatformConfiguration} instance
     */
    private void createPodDecorators(DecoratorsContext context, C config) {
        final var name = context.name();
        config.imagePullSecrets().ifPresent(l -> l.forEach(s -> context.add(new AddImagePullSecretDecorator(name, s))));

        config.hostAliases().entrySet()
                .forEach(e -> context.add(new AddHostAliasesDecorator(name, HostAliasConverter.convert(e))));

        config.nodeSelector().ifPresent(n -> context.add(new AddNodeSelectorDecorator(name, n.key(), n.value())));

        config.initContainers().entrySet()
                .forEach(e -> context.add(new AddInitContainerDecorator(name, ContainerConverter.convert(e))));

        config.sidecars().entrySet().forEach(e -> context.add(new AddSidecarDecorator(name, ContainerConverter.convert(e))));

        config.resources().limits().cpu().ifPresent(c -> context.add(new ApplyLimitsCpuDecorator(name, c)));

        config.resources().limits().memory().ifPresent(m -> context.add(new ApplyLimitsMemoryDecorator(name, m)));

        config.resources().requests().cpu().ifPresent(c -> context.add(new ApplyRequestsCpuDecorator(name, c)));

        config.resources().requests().memory().ifPresent(m -> context.add(new ApplyRequestsMemoryDecorator(name, m)));

        if (config.securityContext().isAnyPropertySet()) {
            context.add(new ApplySecuritySettingsDecorator(name, config.securityContext()));
        }
    }

    private void createAppConfigVolumeAndEnvDecorators(DecoratorsContext context, C config) {
        Set<String> paths = new HashSet<>();

        config.appSecret().ifPresent(s -> {
            context.add(new AddSecretVolumeDecorator(new SecretVolumeBuilder()
                    .withSecretName(s)
                    .withVolumeName("app-secret")
                    .build()));
            context.add(new AddMountDecorator(new MountBuilder()
                    .withName("app-secret")
                    .withPath("/mnt/app-secret")
                    .build()));
            paths.add("/mnt/app-secret");
        });

        config.appConfigMap().ifPresent(s -> {
            context.add(new AddConfigMapVolumeDecorator(new ConfigMapVolumeBuilder()
                    .withConfigMapName(s)
                    .withVolumeName("app-config-map")
                    .build()));
            context.add(new AddMountDecorator(new MountBuilder()
                    .withName("app-config-map")
                    .withPath("/mnt/app-config-map")
                    .build()));
            paths.add("/mnt/app-config-map");
        });

        if (!paths.isEmpty()) {
            context.add(new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, context.name, new EnvBuilder()
                    .withName("SMALLRYE_CONFIG_LOCATIONS")
                    .withValue(String.join(",", paths))
                    .build()));
        }
    }

    private void createMountAndVolumeDecorators(DecoratorsContext context, C config) {
        config.mounts().entrySet()
                .forEach(e -> context.add(new AddMountDecorator(ANY, context.name, MountConverter.convert(e))));

        config.secretVolumes().entrySet()
                .forEach(e -> context.add(new AddSecretVolumeDecorator(SecretVolumeConverter.convert(e))));

        config.configMapVolumes().entrySet()
                .forEach(e -> context.add(new AddConfigMapVolumeDecorator(ConfigMapVolumeConverter.convert(e))));

        config.emptyDirVolumes().ifPresent(volumes -> volumes
                .forEach(e -> context.add(new AddEmptyDirVolumeDecorator(EmptyDirVolumeConverter.convert(e)))));

        config.pvcVolumes().entrySet().forEach(e -> context.add(new AddPvcVolumeDecorator(PvcVolumeConverter.convert(e))));

        config.awsElasticBlockStoreVolumes().entrySet().forEach(
                e -> context.add(new AddAwsElasticBlockStoreVolumeDecorator(AwsElasticBlockStoreVolumeConverter.convert(e))));

        config.azureFileVolumes().entrySet()
                .forEach(e -> context.add(new AddAzureFileVolumeDecorator(AzureFileVolumeConverter.convert(e))));

        config.azureDiskVolumes().entrySet()
                .forEach(e -> context.add(new AddAzureDiskVolumeDecorator(AzureDiskVolumeConverter.convert(e))));
    }

    private void createAnnotationDecorators(DecoratorsContext context, Optional<Project> project,
            C config,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            List<KubernetesAnnotationBuildItem> annotations,
            Optional<Port> port) {
        final var name = context.name();
        annotations
                .forEach(a -> context.addToTarget(new AddAnnotationDecorator(name, a.getKey(), a.getValue()), a.getTarget()));

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        project.ifPresent(p -> {
            ScmInfo scm = p.getScmInfo();
            String vcsUri = parseVCSUri(config.vcsUri(), scm);
            String commitId = scm != null ? scm.getCommit() : null;

            // Dekorate uses its own annotations. Let's replace them with the quarkus ones.
            context.add(new RemoveAnnotationDecorator(Annotations.VCS_URL));
            context.add(new RemoveAnnotationDecorator(Annotations.COMMIT_ID));

            context.add(new AddAnnotationDecorator(name,
                    new Annotation(QUARKUS_ANNOTATIONS_QUARKUS_VERSION, Version.getVersion(), new String[0])));
            //Add quarkus vcs annotations
            if (commitId != null && !config.idempotent()) {
                context.add(new AddAnnotationDecorator(name,
                        new Annotation(QUARKUS_ANNOTATIONS_COMMIT_ID, commitId, new String[0])));
            }
            if (vcsUri != null) {
                context.add(
                        new AddAnnotationDecorator(name, new Annotation(QUARKUS_ANNOTATIONS_VCS_URL, vcsUri, new String[0])));
            }

        });

        if (config.addBuildTimestamp() && !config.idempotent()) {
            context.add(new AddAnnotationDecorator(name, new Annotation(QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP,
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss Z")), new String[0])));
        }

        // Add metrics annotations
        metricsConfiguration.ifPresent(m -> {
            String path = m.metricsEndpoint();
            String prefix = config.prometheus().prefix();
            if (port.isPresent() && path != null) {
                if (config.prometheus().generateServiceMonitor()) {
                    context.add(new AddServiceMonitorResourceDecorator(
                            config.prometheus().scheme().orElse("http"),
                            config.prometheus().port().orElse(String.valueOf(port.get().getContainerPort())),
                            config.prometheus().path().orElse(path),
                            10,
                            true));
                }

                if (config.prometheus().annotations()) {
                    context.add(new AddAnnotationDecorator(name,
                            config.prometheus().scrape().orElse(prefix + "/scrape"), "true",
                            PROMETHEUS_ANNOTATION_TARGETS));
                    context.add(new AddAnnotationDecorator(name,
                            config.prometheus().path().orElse(prefix + "/path"), path, PROMETHEUS_ANNOTATION_TARGETS));

                    final var managementPort = ConfigProvider.getConfig()
                            .getOptionalValue("quarkus.management.port", Integer.class).orElse(9000);
                    final var prometheusPort = KubernetesConfigUtil.managementPortIsEnabled() ? managementPort
                            : port.get().getContainerPort();
                    context.add(new AddAnnotationDecorator(name,
                            config.prometheus().port().orElse(prefix + "/port"), "" + prometheusPort,
                            PROMETHEUS_ANNOTATION_TARGETS));
                    context.add(new AddAnnotationDecorator(name,
                            config.prometheus().scheme().orElse(prefix + "/scheme"), "http",
                            PROMETHEUS_ANNOTATION_TARGETS));
                }
            }
        });
    }

    private static final String[] PROMETHEUS_ANNOTATION_TARGETS = { "Service",
            "Deployment", "DeploymentConfig" };

    /**
     * Create the decorators needed for setting up probes.
     * The method will not create decorators related to ports, as they are not supported by all targets (e.g. knative)
     * Port related decorators are created by `applyProbePort` instead.
     */
    private void createProbeDecorators(DecoratorsContext context, ProbeConfig livenessProbe,
            ProbeConfig readinessProbe,
            ProbeConfig startupProbe,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath) {
        createLivenessProbe(context.name, livenessProbe, livenessPath).ifPresent(context::add);
        createReadinessProbe(context.name, readinessProbe, readinessPath).ifPresent(context::add);
        if (!KNATIVE.equals(context.target)) { // see https://github.com/quarkusio/quarkus/issues/33944
            createStartupProbe(context.name, startupProbe, startupPath).ifPresent(context::add);
        }
    }

    private static Optional<AddLivenessProbeDecorator> createLivenessProbe(String name, ProbeConfig livenessProbe,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath) {
        if (livenessProbe.hasUserSuppliedAction()) {
            return Optional.of(new AddLivenessProbeDecorator(name, ProbeConverter.convert(name, livenessProbe)));
        } else if (livenessPath.isPresent()) {
            return Optional.of(new AddLivenessProbeDecorator(name,
                    ProbeConverter.builder(name, livenessProbe).withHttpActionPath(livenessPath.get().getPath()).build()));
        }
        return Optional.empty();
    }

    private static Optional<AddReadinessProbeDecorator> createReadinessProbe(String name, ProbeConfig readinessProbe,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath) {
        if (readinessProbe.hasUserSuppliedAction()) {
            return Optional.of(new AddReadinessProbeDecorator(name, ProbeConverter.convert(name, readinessProbe)));
        } else if (readinessPath.isPresent()) {
            return Optional.of(new AddReadinessProbeDecorator(name,
                    ProbeConverter.builder(name, readinessProbe).withHttpActionPath(readinessPath.get().getPath()).build()));
        }
        return Optional.empty();
    }

    private static Optional<AddStartupProbeDecorator> createStartupProbe(String name, ProbeConfig startupProbe,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath) {
        if (startupProbe.hasUserSuppliedAction()) {
            return Optional.of(new AddStartupProbeDecorator(name, ProbeConverter.convert(name, startupProbe)));
        } else if (startupPath.isPresent()) {
            return Optional.of(new AddStartupProbeDecorator(name,
                    ProbeConverter.builder(name, startupProbe).withHttpActionPath(startupPath.get().getPath()).build()));
        }
        return Optional.empty();
    }

    private static List<PolicyRule> toPolicyRulesList(Map<String, RbacConfig.PolicyRuleConfig> policyRules) {
        return policyRules.values().stream().map(RBACUtil::from).toList();
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

        createDecorators(context, project, namespace, config,
                metricsConfiguration, kubernetesClientConfiguration,
                annotations, labels, image, command,
                port, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                clusterRoleBindings);

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
