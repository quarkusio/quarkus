package io.quarkus.kubernetes.deployment;

import static io.dekorate.kubernetes.decorator.AddServiceResourceDecorator.distinct;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.PortBuilder;
import io.dekorate.project.BuildInfo;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.project.ScmInfo;
import io.dekorate.utils.Git;
import io.dekorate.utils.Strings;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEffectiveServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.Property;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class KubernetesCommonHelper {
    private static final Logger LOG = Logger.getLogger(KubernetesCommonHelper.class);
    private static final String OUTPUT_ARTIFACT_FORMAT = "%s%s.jar";

    public static Optional<Project> createProject(ApplicationInfoBuildItem app,
            Optional<CustomProjectRootBuildItem> customProjectRoot, OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig) {
        return createProject(app, customProjectRoot, outputTarget.getOutputDirectory()
                .resolve(String.format(OUTPUT_ARTIFACT_FORMAT, outputTarget.getBaseName(),
                        packageConfig.computedRunnerSuffix())));
    }

    public static Optional<Project> createProject(ApplicationInfoBuildItem app,
            Optional<CustomProjectRootBuildItem> customProjectRoot, Path artifactPath) {
        //Let dekorate create a Project instance and then override with what is found in ApplicationInfoBuildItem.
        final var name = app.getName();
        try {
            Project project = FileProjectFactory.create(artifactPath.toFile());
            BuildInfo buildInfo = new BuildInfo(name, app.getVersion(),
                    "jar", project.getBuildInfo().getBuildTool(),
                    project.getBuildInfo().getBuildToolVersion(),
                    artifactPath.toAbsolutePath(),
                    project.getBuildInfo().getClassOutputDir(),
                    project.getBuildInfo().getResourceDir());

            return Optional
                    .of(new Project(customProjectRoot.isPresent() ? customProjectRoot.get().getRoot() : project.getRoot(),
                            buildInfo, project.getScmInfo()));

        } catch (Exception e) {
            LOG.debugv(e, "Couldn't create project for {0} application", name);
            return Optional.empty();
        }
    }

    /**
     * Creates the configurator build items.
     */
    public static Optional<Port> getPort(List<KubernetesPortBuildItem> ports, KubernetesConfig config) {
        return getPort(ports, config, config.ingress().targetPort());
    }

    /**
     * Creates the configurator build items.
     */
    public static Optional<Port> getPort(List<KubernetesPortBuildItem> ports, PlatformConfiguration config, String targetPort) {
        return combinePorts(ports, config).values().stream()
                .filter(distinct(Port::getName))
                .filter(p -> p.getName().equals(targetPort))
                .findFirst();
    }

    /**
     * Creates the configurator build items.
     */
    public static Map<String, Port> combinePorts(List<KubernetesPortBuildItem> ports,
            PlatformConfiguration config) {
        Map<String, Port> allPorts = new HashMap<>();
        Map<String, Port> activePorts = new HashMap<>();

        allPorts.putAll(ports.stream()
                .map(p -> new PortBuilder().withName(p.getName()).withContainerPort(p.getPort()).build())
                .collect(Collectors.toMap(Port::getName, Function.identity(), (first, second) -> first))); //prevent dublicate keys

        activePorts.putAll(verifyPorts(ports)
                .entrySet().stream()
                .map(e -> new PortBuilder().withName(e.getKey()).withContainerPort(e.getValue()).build())
                .collect(Collectors.toMap(Port::getName, Function.identity(), (first, second) -> first))); //prevent dublicate keys

        config.ports().entrySet().forEach(e -> {
            String name = e.getKey();
            Port configuredPort = PortConverter.convert(e);
            Port buildItemPort = allPorts.get(name);

            Port combinedPort = buildItemPort == null ? configuredPort
                    : new PortBuilder()
                            .withName(name)
                            .withHostPort(configuredPort.getHostPort() != null && configuredPort.getHostPort() != 0
                                    ? configuredPort.getHostPort()
                                    : buildItemPort.getHostPort())
                            .withContainerPort(
                                    configuredPort.getContainerPort() != null && configuredPort.getContainerPort() != 0
                                            ? configuredPort.getContainerPort()
                                            : buildItemPort.getContainerPort())
                            .withPath(Strings.isNotNullOrEmpty(configuredPort.getPath()) ? configuredPort.getPath()
                                    : buildItemPort.getPath())
                            .build();
            activePorts.put(name, combinedPort);
        });
        return activePorts;
    }

    private static Map<String, Integer> verifyPorts(List<KubernetesPortBuildItem> kubernetesPortBuildItems) {
        final Map<String, Integer> result = new HashMap<>();
        final Set<Integer> usedPorts = new HashSet<>();
        for (KubernetesPortBuildItem entry : kubernetesPortBuildItems) {
            if (!entry.isEnabled()) {
                continue;
            }
            final String name = entry.getName();
            if (result.containsKey(name)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must have unique names - " + name + " has been used multiple times");
            }
            final Integer port = entry.getPort();
            if (usedPorts.contains(port)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must be unique - " + port + " has been used multiple times");
            }
            result.put(name, port);
            usedPorts.add(port);
        }
        return result;
    }

    /**
     * Creates the configurator build items.
     */
    public static void printMessageAboutPortsThatCantChange(String target, List<KubernetesPortBuildItem> ports,
            PlatformConfiguration configuration) {
        ports.forEach(port -> {
            boolean enabled = port.isEnabled() || configuration.ports().containsKey(port.getName());
            if (enabled) {
                String name = "quarkus." + target + ".ports." + port.getName() + ".container-port";
                Optional<Integer> value = Optional.ofNullable(configuration.ports().get(port.getName()))
                        .map(PortConfig::containerPort)
                        .filter(OptionalInt::isPresent)
                        .map(OptionalInt::getAsInt);
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Property<Integer> kubernetesPortProperty = new Property(name, Integer.class, value, null, false);
                PropertyUtil.printMessages(String.format("The container port %s", port.getName()), target,
                        kubernetesPortProperty,
                        port.getSource());
            }
        });
    }

    public static KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccount(String name, String target,
            PlatformConfiguration config, List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        Optional<String> effectiveServiceAccount = Optional.empty();
        String effectiveServiceAccountNamespace = null;
        for (KubernetesServiceAccountBuildItem sa : serviceAccountsFromExtensions) {
            String saName = Optional.ofNullable(sa.getName()).orElse(name);
            decorators.produce(new DecoratorBuildItem(target, new AddServiceAccountResourceDecorator(name, saName,
                    sa.getNamespace(),
                    sa.getLabels())));

            if (sa.isUseAsDefault() || effectiveServiceAccount.isEmpty()) {
                effectiveServiceAccount = Optional.of(saName);
                effectiveServiceAccountNamespace = sa.getNamespace();
            }
        }

        // Add service account from configuration
        for (Map.Entry<String, RbacConfig.ServiceAccountConfig> sa : config.rbac().serviceAccounts().entrySet()) {
            String saName = sa.getValue().name().orElse(sa.getKey());
            decorators.produce(new DecoratorBuildItem(target, new AddServiceAccountResourceDecorator(name, saName,
                    sa.getValue().namespace().orElse(null),
                    sa.getValue().labels())));

            if (sa.getValue().isUseAsDefault() || effectiveServiceAccount.isEmpty()) {
                effectiveServiceAccount = Optional.of(saName);
                effectiveServiceAccountNamespace = sa.getValue().namespace().orElse(null);
            }
        }

        // The user provided service account should always take precedence
        if (config.serviceAccount().isPresent()) {
            effectiveServiceAccount = config.serviceAccount();
            effectiveServiceAccountNamespace = null;
        }

        final var effectiveName = effectiveServiceAccount.orElse(name);
        return new KubernetesEffectiveServiceAccountBuildItem(effectiveName, effectiveServiceAccountNamespace,
                effectiveServiceAccount.isPresent(), target);
    }

    static String parseVCSUri(VCSUriConfig config, ScmInfo scm) {
        if (!config.enabled()) {
            return null;
        }
        if (config.override().isPresent()) {
            return config.override().get();
        }
        if (scm == null) {
            return null;
        }
        String originRemote = scm.getRemote().get("origin");
        if (originRemote == null || originRemote.isBlank()) {
            return null;
        }
        return Git.sanitizeRemoteUrl(originRemote);
    }
}
