package io.quarkus.smallrye.health.deployment;

import static io.quarkus.arc.processor.Annotations.containsAny;
import static io.quarkus.arc.processor.Annotations.getAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.deployment.util.WebJarUtil;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthStartupPathBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
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
    private static final DotName JAX_RS_PATH = DotName.createSimple("javax.ws.rs.Path");

    // For the UI
    private static final String HEALTH_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String HEALTH_UI_WEBJAR_ARTIFACT_ID = "smallrye-health-ui";
    private static final String HEALTH_UI_WEBJAR_PREFIX = "META-INF/resources/health-ui/";
    private static final String HEALTH_UI_FINAL_DESTINATION = "META-INF/health-ui-files";
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
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation)
            throws IOException, ClassNotFoundException {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_HEALTH));

        // Discover the beans annotated with @Health, @Liveness, @Readiness, @Startup, @HealthGroup,
        // @HealthGroups and @Wellness even if no scope is defined
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(LIVENESS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(READINESS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(STARTUP));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUP));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUPS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(WELLNESS));

        // Add additional beans
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
                .route(healthConfig.rootPath)
                .routeConfigKey("quarkus.smallrye-health.root-path")
                .handler(new SmallRyeHealthHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());

        // Register the liveness handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute(healthConfig.rootPath, healthConfig.livenessPath)
                .handler(new SmallRyeLivenessHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());

        // Register the readiness handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute(healthConfig.rootPath, healthConfig.readinessPath)
                .handler(new SmallRyeReadinessHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());

        // Find all health groups
        Set<String> healthGroups = new HashSet<>();
        // with simple @HealthGroup annotations
        for (AnnotationInstance healthGroupAnnotation : index.getAnnotations(HEALTH_GROUP)) {
            healthGroups.add(healthGroupAnnotation.value().asString());
        }
        // with @HealthGroups repeatable annotations
        for (AnnotationInstance healthGroupsAnnotation : index.getAnnotations(HEALTH_GROUPS)) {
            for (AnnotationInstance healthGroupAnnotation : healthGroupsAnnotation.value().asNestedArray()) {
                healthGroups.add(healthGroupAnnotation.value().asString());
            }
        }

        // Register the health group handlers
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute(healthConfig.rootPath, healthConfig.groupPath)
                .handler(new SmallRyeHealthGroupHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());

        SmallRyeIndividualHealthGroupHandler handler = new SmallRyeIndividualHealthGroupHandler();
        for (String healthGroup : healthGroups) {
            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .nestedRoute(healthConfig.rootPath, healthConfig.groupPath + "/" + healthGroup)
                    .handler(handler)
                    .displayOnNotFoundPage()
                    .blockingRoute()
                    .build());
        }

        // Register the wellness handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute(healthConfig.rootPath, healthConfig.wellnessPath)
                .handler(new SmallRyeWellnessHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());

        // Register the startup handler
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute(healthConfig.rootPath, healthConfig.startupPath)
                .handler(new SmallRyeStartupHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());

    }

    @BuildStep
    public void translateSmallRyeConfigValues(SmallRyeHealthConfig healthConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {
        if (healthConfig.contextPropagation) {
            systemProperties.produce(new SystemPropertyBuildItem("io.smallrye.health.context.propagation", "true"));
        }
    }

    @BuildStep(onlyIf = OpenAPIIncluded.class)
    public void includeInOpenAPIEndpoint(BuildProducer<AddToOpenAPIDefinitionBuildItem> openAPIProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            Capabilities capabilities,
            SmallRyeHealthConfig healthConfig) {

        // Add to OpenAPI if OpenAPI is available
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            String healthRootPath = nonApplicationRootPathBuildItem.resolvePath(healthConfig.rootPath);

            HealthOpenAPIFilter filter = new HealthOpenAPIFilter(healthRootPath,
                    nonApplicationRootPathBuildItem.resolveNestedPath(healthRootPath, healthConfig.livenessPath),
                    nonApplicationRootPathBuildItem.resolveNestedPath(healthRootPath, healthConfig.readinessPath),
                    nonApplicationRootPathBuildItem.resolveNestedPath(healthRootPath, healthConfig.startupPath));

            openAPIProducer.produce(new AddToOpenAPIDefinitionBuildItem(filter));
        }
    }

    private void warnIfJaxRsPathUsed(IndexView index, DotName healthAnnotation) {
        Collection<AnnotationInstance> instances = index.getAnnotations(healthAnnotation);
        for (AnnotationInstance instance : instances) {
            boolean containsPath = false;

            AnnotationTarget target = instance.target();
            if (target.kind() == Kind.CLASS) {
                if (target.asClass().classAnnotation(JAX_RS_PATH) != null) {
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
            BuildProducer<KubernetesHealthLivenessPathBuildItem> livenessPathItemProducer,
            BuildProducer<KubernetesHealthReadinessPathBuildItem> readinessPathItemProducer,
            BuildProducer<KubernetesHealthStartupPathBuildItem> startupPathItemProducer) {

        livenessPathItemProducer.produce(
                new KubernetesHealthLivenessPathBuildItem(
                        nonApplicationRootPathBuildItem.resolveNestedPath(healthConfig.rootPath, healthConfig.livenessPath)));
        readinessPathItemProducer.produce(
                new KubernetesHealthReadinessPathBuildItem(
                        nonApplicationRootPathBuildItem.resolveNestedPath(healthConfig.rootPath, healthConfig.readinessPath)));
        startupPathItemProducer.produce(
                new KubernetesHealthStartupPathBuildItem(
                        nonApplicationRootPathBuildItem.resolveNestedPath(healthConfig.rootPath, healthConfig.startupPath)));
    }

    @BuildStep
    ShutdownListenerBuildItem shutdownListener() {
        return new ShutdownListenerBuildItem(new ShutdownReadinessListener());
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer(BeanArchiveIndexBuildItem beanArchiveIndex,
            CustomScopeAnnotationsBuildItem scopes) {

        // Transform health checks that are not annotated with a scope or a stereotype with a default scope
        Set<DotName> stereotypeAnnotations = new HashSet<>();
        for (AnnotationInstance annotation : beanArchiveIndex.getIndex().getAnnotations(DotNames.STEREOTYPE)) {
            ClassInfo annotationClass = beanArchiveIndex.getIndex().getClassByName(annotation.name());
            if (annotationClass != null && scopes.isScopeIn(annotationClass.classAnnotations())) {
                // Stereotype annotation with a default scope
                stereotypeAnnotations.add(annotationClass.name());
            }
        }
        List<DotName> healthAnnotations = new ArrayList<>(5);
        healthAnnotations.add(LIVENESS);
        healthAnnotations.add(READINESS);
        healthAnnotations.add(STARTUP);
        healthAnnotations.add(HEALTH_GROUP);
        healthAnnotations.add(HEALTH_GROUPS);
        healthAnnotations.add(WELLNESS);

        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS || kind == Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.getAnnotations().isEmpty()) {
                    return;
                }
                Collection<AnnotationInstance> annotations;
                if (ctx.isClass()) {
                    annotations = ctx.getAnnotations();
                    if (containsAny(annotations, stereotypeAnnotations)) {
                        return;
                    }
                } else {
                    annotations = getAnnotations(Kind.METHOD, ctx.getAnnotations());
                }
                if (scopes.isScopeIn(annotations)) {
                    return;
                }
                if (containsAny(annotations, healthAnnotations)) {
                    ctx.transform().add(BuiltinScope.SINGLETON.getName()).done();
                }
            }

        });
    }

    // UI
    @BuildStep
    void registerUiExtension(
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer,
            BuildProducer<SmallRyeHealthBuildItem> smallRyeHealthBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeHealthConfig healthConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReloadBuildItem) throws Exception {

        if (shouldInclude(launchMode, healthConfig)) {

            if ("/".equals(healthConfig.ui.rootPath)) {
                throw new ConfigurationError(
                        "quarkus.smallrye-health.root-path-ui was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
            }

            String healthPath = nonApplicationRootPathBuildItem.resolvePath(healthConfig.rootPath);
            String healthUiPath = nonApplicationRootPathBuildItem.resolvePath(healthConfig.ui.rootPath);

            AppArtifact artifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, HEALTH_UI_WEBJAR_GROUP_ID,
                    HEALTH_UI_WEBJAR_ARTIFACT_ID);

            if (launchMode.getLaunchMode().isDevOrTest()) {
                Path tempPath = WebJarUtil.copyResourcesForDevOrTest(liveReloadBuildItem, curateOutcomeBuildItem, launchMode,
                        artifact,
                        HEALTH_UI_WEBJAR_PREFIX);
                updateApiUrl(tempPath.resolve(JS_FILE_TO_UPDATE), healthPath);
                updateApiUrl(tempPath.resolve(INDEX_FILE_TO_UPDATE), healthPath);

                smallRyeHealthBuildProducer
                        .produce(new SmallRyeHealthBuildItem(tempPath.toAbsolutePath().toString(), healthUiPath));

                // Handle live reload of branding files
                if (liveReloadBuildItem.isLiveReload() && !liveReloadBuildItem.getChangedResources().isEmpty()) {
                    WebJarUtil.hotReloadBrandingChanges(curateOutcomeBuildItem, launchMode, artifact,
                            liveReloadBuildItem.getChangedResources());
                }
            } else {
                Map<String, byte[]> files = WebJarUtil.copyResourcesForProduction(curateOutcomeBuildItem, artifact,
                        HEALTH_UI_WEBJAR_PREFIX);

                for (Map.Entry<String, byte[]> file : files.entrySet()) {

                    String fileName = file.getKey();
                    byte[] content = file.getValue();
                    if (fileName.endsWith(JS_FILE_TO_UPDATE) || fileName.endsWith(INDEX_FILE_TO_UPDATE)) {
                        content = updateApiUrl(new String(content, StandardCharsets.UTF_8), healthPath)
                                .getBytes(StandardCharsets.UTF_8);
                    }
                    fileName = HEALTH_UI_FINAL_DESTINATION + "/" + fileName;

                    generatedResourceProducer.produce(new GeneratedResourceBuildItem(fileName, content));
                    nativeImageResourceProducer.produce(new NativeImageResourceBuildItem(fileName));
                }

                smallRyeHealthBuildProducer.produce(new SmallRyeHealthBuildItem(HEALTH_UI_FINAL_DESTINATION, healthUiPath));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerHealthUiHandler(
            BuildProducer<RouteBuildItem> routeProducer,
            SmallRyeHealthRecorder recorder,
            SmallRyeHealthRuntimeConfig runtimeConfig,
            SmallRyeHealthBuildItem smallRyeHealthBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeHealthConfig healthConfig) {

        if (shouldInclude(launchMode, healthConfig)) {
            Handler<RoutingContext> handler = recorder.uiHandler(smallRyeHealthBuildItem.getHealthUiFinalDestination(),
                    smallRyeHealthBuildItem.getHealthUiPath(), runtimeConfig);
            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(healthConfig.ui.rootPath)
                    .displayOnNotFoundPage("Health UI")
                    .routeConfigKey("quarkus.smallrye-health.ui.root-path")
                    .handler(handler)
                    .build());

            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(healthConfig.ui.rootPath + "*")
                    .handler(handler)
                    .build());

        }
    }

    private void updateApiUrl(Path fileToUpdate, String healthPath) throws IOException {
        String content = new String(Files.readAllBytes(fileToUpdate), StandardCharsets.UTF_8);
        String result = updateApiUrl(content, healthPath);
        if (result != null) {
            Files.write(fileToUpdate, result.getBytes(StandardCharsets.UTF_8));
        }
    }

    // Replace health URL in static files
    public String updateApiUrl(String original, String healthPath) {
        return original.replace("url = \"/health\";", "url = \"" + healthPath + "\";")
                .replace("placeholder=\"/health\"", "placeholder=\"" + healthPath + "\"");
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, SmallRyeHealthConfig healthConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || healthConfig.ui.alwaysInclude;
    }
}
