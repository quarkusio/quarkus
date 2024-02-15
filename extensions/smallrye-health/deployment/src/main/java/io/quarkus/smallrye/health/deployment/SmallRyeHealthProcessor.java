package io.quarkus.smallrye.health.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthStartupPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesProbePortNameBuildItem;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.health.runtime.QuarkusAsyncHealthCheckFactory;
import io.quarkus.smallrye.health.runtime.ShutdownReadinessListener;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthGroupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRecorder;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRuntimeConfig;
import io.quarkus.smallrye.health.runtime.SmallRyeIndividualHealthGroupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeLivenessHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeReadinessHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeStartupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeWellnessHandler;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResourcesFilter;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.smallrye.health.AsyncHealthCheckFactory;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.health.api.HealthGroup;
import io.smallrye.health.api.HealthGroups;
import io.smallrye.health.api.Wellness;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class SmallRyeHealthProcessor {
    private static final Logger LOG = Logger.getLogger(SmallRyeHealthProcessor.class);

    private static final DotName LIVENESS = DotName.createSimple(Liveness.class.getName());
    private static final DotName READINESS = DotName.createSimple(Readiness.class.getName());
    private static final DotName STARTUP = DotName.createSimple(Startup.class.getName());
    private static final DotName HEALTH_GROUP = DotName.createSimple(HealthGroup.class.getName());
    private static final DotName HEALTH_GROUPS = DotName.createSimple(HealthGroups.class.getName());
    private static final DotName WELLNESS = DotName.createSimple(Wellness.class.getName());
    private static final DotName JAX_RS_PATH = DotName.createSimple("jakarta.ws.rs.Path");

    // For the UI
    private static final GACT HEALTH_UI_WEBJAR_ARTIFACT_KEY = new GACT("io.smallrye", "smallrye-health-ui", null, "jar");
    private static final String HEALTH_UI_WEBJAR_STATIC_RESOURCES_PATH = "META-INF/resources/health-ui/";
    private static final String JS_FILE_TO_UPDATE = "healthui.js";
    private static final String INDEX_FILE_TO_UPDATE = "index.html";

    // Branding files to monitor for changes
    private static final String BRANDING_DIR = "META-INF/branding/";
    private static final String BRANDING_LOGO_GENERAL = BRANDING_DIR + "logo.png";
    private static final String BRANDING_LOGO_MODULE = BRANDING_DIR + "smallrye-health-ui.png";
    private static final String BRANDING_STYLE_GENERAL = BRANDING_DIR + "style.css";
    private static final String BRANDING_STYLE_MODULE = BRANDING_DIR + "smallrye-health-ui.css";
    private static final String BRANDING_FAVICON_GENERAL = BRANDING_DIR + "favicon.ico";
    private static final String BRANDING_FAVICON_MODULE = BRANDING_DIR + "smallrye-health-ui.ico";

    // For Kubernetes exposing
    private static final String SCHEME_HTTP = "HTTP";
    private static final String SCHEME_HTTPS = "HTTPS";

    // For Management ports
    private static final String MANAGEMENT_SSL_PREFIX = "quarkus.management.ssl.certificate.";
    private static final List<String> MANAGEMENT_SSL_PROPERTIES = List.of("key-store-file", "trust-store-file", "files",
            "key-files");

    static class OpenAPIIncluded implements BooleanSupplier {
        HealthBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.openapiIncluded;
        }
    }

    HealthBuildTimeConfig config;

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> brandingFiles() {
        return Stream.of(BRANDING_LOGO_GENERAL,
                BRANDING_STYLE_GENERAL,
                BRANDING_FAVICON_GENERAL,
                BRANDING_LOGO_MODULE,
                BRANDING_STYLE_MODULE,
                BRANDING_FAVICON_MODULE).map(HotDeploymentWatchedFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    void healthCheck(BuildProducer<AdditionalBeanBuildItem> buildItemBuildProducer,
            List<HealthBuildItem> healthBuildItems) {
        boolean extensionsEnabled = config.extensionsEnabled &&
                !ConfigProvider.getConfig().getOptionalValue("mp.health.disable-default-procedures", boolean.class)
                        .orElse(false);
        if (extensionsEnabled) {
            for (HealthBuildItem buildItem : healthBuildItems) {
                if (buildItem.isEnabled()) {
                    buildItemBuildProducer.produce(new AdditionalBeanBuildItem(buildItem.getHealthCheckClass()));
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    void build(SmallRyeHealthRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ExcludedTypeBuildItem> excludedTypes,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation)
            throws IOException, ClassNotFoundException {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_HEALTH));

        // Discover the beans annotated with @Health, @Liveness, @Readiness, @Startup, @HealthGroup,
        // @HealthGroups and @Wellness even if no scope is defined
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(LIVENESS, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(READINESS, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(STARTUP, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUP, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUPS, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(WELLNESS, BuiltinScope.SINGLETON.getName()));

        // Add additional beans
        additionalBean.produce(new AdditionalBeanBuildItem(QuarkusAsyncHealthCheckFactory.class));
        excludedTypes.produce(new ExcludedTypeBuildItem(AsyncHealthCheckFactory.class.getName()));
        additionalBean.produce(new AdditionalBeanBuildItem(SmallRyeHealthReporter.class));

        // Make ArC discover @HealthGroup as a qualifier
        additionalBean.produce(new AdditionalBeanBuildItem(HealthGroup.class));

        // Discover and register the HealthCheckResponseProvider
        Set<String> providers = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + HealthCheckResponseProvider.class.getName());

        if (providers.isEmpty()) {
            throw new IllegalStateException("No HealthCheckResponseProvider implementation found.");
        } else if (providers.size() > 1) {
            throw new IllegalStateException(
                    String.format("Multiple HealthCheckResponseProvider implementations found: %s", providers));
        }

        final String provider = providers.iterator().next();
        final Class<? extends HealthCheckResponseProvider> responseProvider = (Class<? extends HealthCheckResponseProvider>) Class
                .forName(provider, true, Thread.currentThread().getContextClassLoader());
        recorder.registerHealthCheckResponseProvider(responseProvider);
    }

    @BuildStep
    public void defineHealthRoutes(BuildProducer<RouteBuildItem> routes,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeHealthConfig healthConfig) {
        IndexView index = beanArchiveIndex.getIndex();

        // log a warning if users try to use MP Health annotations with JAX-RS @Path
        warnIfJaxRsPathUsed(index, LIVENESS);
        warnIfJaxRsPathUsed(index, READINESS);
        warnIfJaxRsPathUsed(index, STARTUP);
        warnIfJaxRsPathUsed(index, WELLNESS);

        // Register the health handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .route(healthConfig.rootPath)
                .routeConfigKey("quarkus.smallrye-health.root-path")
                .handler(new SmallRyeHealthHandler())
                .displayOnNotFoundPage()
                .build());

        // Register the liveness handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .nestedRoute(healthConfig.rootPath, healthConfig.livenessPath)
                .handler(new SmallRyeLivenessHandler())
                .displayOnNotFoundPage()
                .build());

        // Register the readiness handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .nestedRoute(healthConfig.rootPath, healthConfig.readinessPath)
                .handler(new SmallRyeReadinessHandler())
                .displayOnNotFoundPage()
                .build());

        // Register the health group handlers
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .nestedRoute(healthConfig.rootPath, healthConfig.groupPath)
                .handler(new SmallRyeHealthGroupHandler())
                .displayOnNotFoundPage()
                .build());

        SmallRyeIndividualHealthGroupHandler handler = new SmallRyeIndividualHealthGroupHandler();
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .nestedRoute(healthConfig.rootPath, healthConfig.groupPath + "/*")
                .handler(handler)
                .displayOnNotFoundPage()
                .build());

        // Register the wellness handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .nestedRoute(healthConfig.rootPath, healthConfig.wellnessPath)
                .handler(new SmallRyeWellnessHandler())
                .displayOnNotFoundPage()
                .build());

        // Register the startup handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-health.management.enabled")
                .nestedRoute(healthConfig.rootPath, healthConfig.startupPath)
                .handler(new SmallRyeStartupHandler())
                .displayOnNotFoundPage()
                .build());

    }

    @BuildStep
    public void processSmallRyeHealthConfigValues(SmallRyeHealthConfig healthConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {
        if (healthConfig.contextPropagation) {
            config.produce(new RunTimeConfigurationDefaultBuildItem("io.smallrye.health.context.propagation", "true"));
        }
        if (healthConfig.maxGroupRegistriesCount.isPresent()) {
            config.produce(new RunTimeConfigurationDefaultBuildItem("io.smallrye.health.maxGroupRegistriesCount",
                    String.valueOf(healthConfig.maxGroupRegistriesCount.getAsInt())));
        }
        config.produce(new RunTimeConfigurationDefaultBuildItem("io.smallrye.health.delayChecksInitializations", "true"));
        if (healthConfig.defaultHealthGroup.isPresent()) {
            config.produce(new RunTimeConfigurationDefaultBuildItem("io.smallrye.health.defaultHealthGroup",
                    healthConfig.defaultHealthGroup.get()));
        }
    }

    @BuildStep(onlyIf = OpenAPIIncluded.class)
    public void includeInOpenAPIEndpoint(BuildProducer<AddToOpenAPIDefinitionBuildItem> openAPIProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            Capabilities capabilities,
            SmallRyeHealthConfig healthConfig) {

        // Add to OpenAPI if OpenAPI is available
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI) && !managementInterfaceBuildTimeConfig.enabled) {
            String healthRootPath = nonApplicationRootPathBuildItem.resolvePath(healthConfig.rootPath);
            HealthOpenAPIFilter filter = new HealthOpenAPIFilter(healthRootPath,
                    nonApplicationRootPathBuildItem.resolveManagementNestedPath(healthRootPath, healthConfig.livenessPath),
                    nonApplicationRootPathBuildItem.resolveManagementNestedPath(healthRootPath, healthConfig.readinessPath),
                    nonApplicationRootPathBuildItem.resolveManagementNestedPath(healthRootPath, healthConfig.startupPath));

            openAPIProducer.produce(new AddToOpenAPIDefinitionBuildItem(filter));
        }
    }

    private void warnIfJaxRsPathUsed(IndexView index, DotName healthAnnotation) {
        Collection<AnnotationInstance> instances = index.getAnnotations(healthAnnotation);
        for (AnnotationInstance instance : instances) {
            boolean containsPath = false;

            AnnotationTarget target = instance.target();
            if (target.kind() == Kind.CLASS) {
                if (target.asClass().declaredAnnotation(JAX_RS_PATH) != null) {
                    containsPath = true;
                }
            } else if (target.kind() == Kind.METHOD) {
                if (target.asMethod().hasAnnotation(JAX_RS_PATH)) {
                    containsPath = true;
                }
            }
            if (containsPath) {
                LOG.warnv(
                        "The use of @Path has no effect when @{0} is used and should therefore be removed. Offending target is {1}: {2}",
                        healthAnnotation.withoutPackagePrefix(), target.kind(), target);
            }
        }
    }

    @BuildStep
    public void kubernetes(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeHealthConfig healthConfig,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            BuildProducer<KubernetesHealthLivenessPathBuildItem> livenessPathItemProducer,
            BuildProducer<KubernetesHealthReadinessPathBuildItem> readinessPathItemProducer,
            BuildProducer<KubernetesHealthStartupPathBuildItem> startupPathItemProducer,
            BuildProducer<KubernetesProbePortNameBuildItem> port) {

        if (managementInterfaceBuildTimeConfig.enabled) {
            // Switch to the "management" port
            port.produce(new KubernetesProbePortNameBuildItem("management", selectSchemeForManagement()));
        }

        livenessPathItemProducer.produce(
                new KubernetesHealthLivenessPathBuildItem(
                        nonApplicationRootPathBuildItem.resolveManagementNestedPath(healthConfig.rootPath,
                                healthConfig.livenessPath)));
        readinessPathItemProducer.produce(
                new KubernetesHealthReadinessPathBuildItem(
                        nonApplicationRootPathBuildItem.resolveManagementNestedPath(healthConfig.rootPath,
                                healthConfig.readinessPath)));
        startupPathItemProducer.produce(
                new KubernetesHealthStartupPathBuildItem(
                        nonApplicationRootPathBuildItem.resolveManagementNestedPath(healthConfig.rootPath,
                                healthConfig.startupPath)));
    }

    @BuildStep
    ShutdownListenerBuildItem shutdownListener() {
        return new ShutdownListenerBuildItem(new ShutdownReadinessListener());
    }

    // UI
    @BuildStep
    void registerUiExtension(
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            SmallRyeHealthConfig healthConfig,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<WebJarBuildItem> webJarBuildProducer) {

        if (shouldInclude(launchModeBuildItem, healthConfig)) {

            if ("/".equals(healthConfig.ui.rootPath)) {
                throw new ConfigurationException(
                        "quarkus.smallrye-health.root-path-ui was set to \"/\", this is not allowed as it blocks the application from serving anything else.",
                        Set.of("quarkus.smallrye-health.root-path-ui"));
            }

            String healthPath = nonApplicationRootPathBuildItem.resolveManagementPath(healthConfig.rootPath,
                    managementInterfaceBuildTimeConfig, launchModeBuildItem);

            webJarBuildProducer.produce(
                    WebJarBuildItem.builder().artifactKey(HEALTH_UI_WEBJAR_ARTIFACT_KEY) //
                            .root(HEALTH_UI_WEBJAR_STATIC_RESOURCES_PATH) //
                            .filter(new WebJarResourcesFilter() {
                                @Override
                                public FilterResult apply(String fileName, InputStream file) throws IOException {
                                    if (fileName.endsWith(JS_FILE_TO_UPDATE) || fileName.endsWith(INDEX_FILE_TO_UPDATE)) {
                                        byte[] content = SmallRyeHealthProcessor.this
                                                .updateApiUrl(new String(file.readAllBytes(), StandardCharsets.UTF_8),
                                                        healthPath)
                                                .getBytes(StandardCharsets.UTF_8);

                                        return new FilterResult(new ByteArrayInputStream(content), true);
                                    }

                                    return new FilterResult(file, false);
                                }
                            })
                            .build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerHealthUiHandler(
            BuildProducer<RouteBuildItem> routeProducer,
            SmallRyeHealthRecorder recorder,
            SmallRyeHealthRuntimeConfig runtimeConfig,
            WebJarResultsBuildItem webJarResultsBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeHealthConfig healthConfig,
            BuildProducer<SmallRyeHealthBuildItem> smallryeHealthBuildProducer, ShutdownContextBuildItem shutdownContext) {

        WebJarResultsBuildItem.WebJarResult result = webJarResultsBuildItem.byArtifactKey(HEALTH_UI_WEBJAR_ARTIFACT_KEY);
        if (result == null) {
            return;
        }

        if (shouldInclude(launchMode, healthConfig)) {
            String healthUiPath = nonApplicationRootPathBuildItem.resolvePath(healthConfig.ui.rootPath);
            smallryeHealthBuildProducer
                    .produce(new SmallRyeHealthBuildItem(result.getFinalDestination(), healthUiPath));

            Handler<RoutingContext> handler = recorder.uiHandler(result.getFinalDestination(),
                    healthUiPath, result.getWebRootConfigurations(), runtimeConfig, shutdownContext);

            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management("quarkus.smallrye-health.management.enabled")
                    .route(healthConfig.ui.rootPath)
                    .displayOnNotFoundPage("Health UI")
                    .routeConfigKey("quarkus.smallrye-health.ui.root-path")
                    .handler(handler)
                    .build());

            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management("quarkus.smallrye-health.management.enabled")
                    .route(healthConfig.ui.rootPath + "*")
                    .handler(handler)
                    .build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void processSmallRyeHealthRuntimeConfig(
            SmallRyeHealthRecorder recorder,
            SmallRyeHealthRuntimeConfig runtimeConfig) {

        recorder.processSmallRyeHealthRuntimeConfiguration(runtimeConfig);
    }

    // Replace health URL in static files
    public String updateApiUrl(String original, String healthPath) {
        return original.replace("url = \"/health\";", "url = \"" + healthPath + "\";")
                .replace("placeholder=\"/health\"", "placeholder=\"" + healthPath + "\"");
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, SmallRyeHealthConfig healthConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || healthConfig.ui.alwaysInclude;
    }

    /**
     * This method will check whether any of the management SSL runtime properties are set at build time.
     * If so, it will select the scheme HTTPS, otherwise HTTP.
     */
    private static String selectSchemeForManagement() {
        Config config = ConfigProvider.getConfig();
        for (String sslProperty : MANAGEMENT_SSL_PROPERTIES) {
            Optional<List<String>> property = config.getOptionalValues(MANAGEMENT_SSL_PREFIX + sslProperty,
                    String.class);
            if (property.isPresent()) {
                return SCHEME_HTTPS;
            }
        }

        return SCHEME_HTTP;
    }
}
