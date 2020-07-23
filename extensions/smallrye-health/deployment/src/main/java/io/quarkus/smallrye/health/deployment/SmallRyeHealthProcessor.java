package io.quarkus.smallrye.health.deployment;

import static io.quarkus.arc.processor.Annotations.containsAny;
import static io.quarkus.arc.processor.Annotations.getAnnotations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
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
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.health.runtime.ShutdownReadinessListener;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthGroupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRecorder;
import io.quarkus.smallrye.health.runtime.SmallRyeIndividualHealthGroupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeLivenessHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeReadinessHandler;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.health.api.HealthGroup;
import io.smallrye.health.api.HealthGroups;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class SmallRyeHealthProcessor {
    private static final Logger LOG = Logger.getLogger(SmallRyeHealthProcessor.class);

    private static final DotName HEALTH = DotName.createSimple(Health.class.getName());
    private static final DotName LIVENESS = DotName.createSimple(Liveness.class.getName());
    private static final DotName READINESS = DotName.createSimple(Readiness.class.getName());
    private static final DotName HEALTH_GROUP = DotName.createSimple(HealthGroup.class.getName());
    private static final DotName HEALTH_GROUPS = DotName.createSimple(HealthGroups.class.getName());
    private static final DotName JAX_RS_PATH = DotName.createSimple("javax.ws.rs.Path");

    // For the UI
    private static final String HEALTH_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String HEALTH_UI_WEBJAR_ARTIFACT_ID = "smallrye-health-ui";
    private static final String HEALTH_UI_WEBJAR_PREFIX = "META-INF/resources/health-ui";
    private static final String OWN_MEDIA_FOLDER = "META-INF/resources/";
    private static final String HEALTH_UI_FINAL_DESTINATION = "META-INF/health-ui-files";
    private static final String TEMP_DIR_PREFIX = "quarkus-health-ui_" + System.nanoTime();
    private static final List<String> IGNORE_LIST = Arrays.asList("logo.png", "favicon.ico");
    private static final String FILE_TO_UPDATE = "healthui.js";
    /**
     * The configuration for health checking.
     */
    SmallRyeHealthConfig health;

    HealthBuildTimeConfig config;

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
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            LaunchModeBuildItem launchModeBuildItem) throws IOException, ClassNotFoundException {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_HEALTH));

        // add health endpoints to not found page
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.livenessPath));
            displayableEndpoints
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.readinessPath));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.groupPath));
        }

        // Discover the beans annotated with @Health, @Liveness, @Readiness, @HealthGroup and @HealthGroups even if no scope is defined
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(LIVENESS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(READINESS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUP));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUPS));

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
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();

        // log a warning if users try to use MP Health annotations with JAX-RS @Path
        warnIfJaxRsPathUsed(index, LIVENESS);
        warnIfJaxRsPathUsed(index, READINESS);
        warnIfJaxRsPathUsed(index, HEALTH);

        // Register the health handler
        routes.produce(new RouteBuildItem(health.rootPath, new SmallRyeHealthHandler(), HandlerType.BLOCKING));

        // Register the liveness handler
        routes.produce(
                new RouteBuildItem(health.rootPath + health.livenessPath, new SmallRyeLivenessHandler(),
                        HandlerType.BLOCKING));

        // Register the readiness handler
        routes.produce(
                new RouteBuildItem(health.rootPath + health.readinessPath, new SmallRyeReadinessHandler(),
                        HandlerType.BLOCKING));

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
        routes.produce(
                new RouteBuildItem(health.rootPath + health.groupPath, new SmallRyeHealthGroupHandler(),
                        HandlerType.BLOCKING));

        SmallRyeIndividualHealthGroupHandler handler = new SmallRyeIndividualHealthGroupHandler();
        for (String healthGroup : healthGroups) {
            routes.produce(
                    new RouteBuildItem(health.rootPath + health.groupPath + "/" + healthGroup,
                            handler, HandlerType.BLOCKING));
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
    public void kubernetes(HttpBuildTimeConfig httpConfig,
            BuildProducer<KubernetesHealthLivenessPathBuildItem> livenessPathItemProducer,
            BuildProducer<KubernetesHealthReadinessPathBuildItem> readinessPathItemProducer) {
        if (httpConfig.rootPath == null) {
            livenessPathItemProducer.produce(new KubernetesHealthLivenessPathBuildItem(health.rootPath + health.livenessPath));
            readinessPathItemProducer
                    .produce(new KubernetesHealthReadinessPathBuildItem(health.rootPath + health.readinessPath));
        } else {
            String basePath = httpConfig.rootPath.replaceAll("/$", "") + health.rootPath;
            livenessPathItemProducer.produce(new KubernetesHealthLivenessPathBuildItem(basePath + health.livenessPath));
            readinessPathItemProducer.produce(new KubernetesHealthReadinessPathBuildItem(basePath + health.readinessPath));
        }
    }

    @BuildStep
    ShutdownListenerBuildItem shutdownListener() {
        return new ShutdownListenerBuildItem(new ShutdownReadinessListener());
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer(BeanArchiveIndexBuildItem beanArchiveIndex,
            CustomScopeAnnotationsBuildItem scopes) {
        // Transform health checks that are not annotated with a scope or a stereotype
        Set<DotName> stereotypes = beanArchiveIndex.getIndex().getAnnotations(DotNames.STEREOTYPE).stream()
                .map(AnnotationInstance::name).collect(Collectors.toSet());
        List<DotName> healthAnnotations = new ArrayList<>(5);
        healthAnnotations.add(HEALTH);
        healthAnnotations.add(LIVENESS);
        healthAnnotations.add(READINESS);
        healthAnnotations.add(HEALTH_GROUP);
        healthAnnotations.add(HEALTH_GROUPS);

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
                    if (containsAny(annotations, stereotypes)) {
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
    @Record(ExecutionTime.STATIC_INIT)
    void registerUiExtension(
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageDisplayableEndpointProducer,
            SmallRyeHealthRecorder recorder,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReload,
            HttpRootPathBuildItem httpRootPath,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {

        if (!health.ui.enable) {
            return;
        }
        if ("/".equals(health.ui.rootPath)) {
            throw new ConfigurationError(
                    "quarkus.smallrye-health.root-path-ui was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
        }

        String healthPath = httpRootPath.adjustPath(health.rootPath);

        if (launchMode.getLaunchMode().isDevOrTest()) {
            CachedHealthUI cached = liveReload.getContextObject(CachedHealthUI.class);
            boolean extractionNeeded = cached == null;

            if (cached != null && !cached.cachedHealthPath.equals(healthPath)) {
                try {
                    FileUtil.deleteDirectory(Paths.get(cached.cachedDirectory));
                } catch (IOException e) {
                    LOG.error("Failed to clean Health UI temp directory on restart", e);
                }
                extractionNeeded = true;
            }
            if (extractionNeeded) {
                if (cached == null) {
                    cached = new CachedHealthUI();
                    liveReload.setContextObject(CachedHealthUI.class, cached);
                    Runtime.getRuntime().addShutdownHook(new Thread(cached, "Health UI Shutdown Hook"));
                }
                try {
                    AppArtifact artifact = getHealthUiArtifact(curateOutcomeBuildItem);
                    Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX).toRealPath();
                    extractHealthUi(artifact, tempDir);
                    updateApiUrl(tempDir.resolve(FILE_TO_UPDATE), healthPath);
                    cached.cachedDirectory = tempDir.toAbsolutePath().toString();
                    cached.cachedHealthPath = healthPath;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            Handler<RoutingContext> handler = recorder.uiHandler(cached.cachedDirectory,
                    httpRootPath.adjustPath(health.ui.rootPath));
            routeProducer.produce(new RouteBuildItem(health.ui.rootPath, handler));
            routeProducer.produce(new RouteBuildItem(health.ui.rootPath + "/*", handler));
            notFoundPageDisplayableEndpointProducer
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(health.ui.rootPath + "/"));
        } else if (health.ui.alwaysInclude) {
            AppArtifact artifact = getHealthUiArtifact(curateOutcomeBuildItem);
            //we are including in a production artifact
            //just stick the files in the generated output
            //we could do this for dev mode as well but then we need to extract them every time
            for (Path p : artifact.getPaths()) {
                File artifactFile = p.toFile();
                try (JarFile jarFile = new JarFile(artifactFile)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(HEALTH_UI_WEBJAR_PREFIX) && !entry.isDirectory()) {
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                String filename = entry.getName().replace(HEALTH_UI_WEBJAR_PREFIX + "/", "");
                                byte[] content = FileUtil.readFileContents(inputStream);
                                if (entry.getName().endsWith(FILE_TO_UPDATE)) {
                                    content = updateApiUrl(new String(content, StandardCharsets.UTF_8), healthPath)
                                            .getBytes(StandardCharsets.UTF_8);
                                }
                                if (IGNORE_LIST.contains(filename)) {
                                    ClassLoader classLoader = SmallRyeHealthProcessor.class.getClassLoader();
                                    try (InputStream resourceAsStream = classLoader
                                            .getResourceAsStream(OWN_MEDIA_FOLDER + filename)) {
                                        content = IoUtil.readBytes(resourceAsStream);
                                    }
                                }

                                String fileName = HEALTH_UI_FINAL_DESTINATION + "/" + filename;

                                generatedResourceProducer
                                        .produce(new GeneratedResourceBuildItem(fileName, content));

                                nativeImageResourceProducer
                                        .produce(new NativeImageResourceBuildItem(fileName));

                            }
                        }
                    }
                }
            }

            Handler<RoutingContext> handler = recorder
                    .uiHandler(HEALTH_UI_FINAL_DESTINATION, httpRootPath.adjustPath(health.ui.rootPath));
            routeProducer.produce(new RouteBuildItem(health.ui.rootPath, handler));
            routeProducer.produce(new RouteBuildItem(health.ui.rootPath + "/*", handler));
        }
    }

    private AppArtifact getHealthUiArtifact(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        for (AppDependency dep : curateOutcomeBuildItem.getEffectiveModel().getFullDeploymentDeps()) {
            if (dep.getArtifact().getArtifactId().equals(HEALTH_UI_WEBJAR_ARTIFACT_ID)
                    && dep.getArtifact().getGroupId().equals(HEALTH_UI_WEBJAR_GROUP_ID)) {
                return dep.getArtifact();
            }
        }
        throw new RuntimeException("Could not find artifact " + HEALTH_UI_WEBJAR_GROUP_ID + ":" + HEALTH_UI_WEBJAR_ARTIFACT_ID
                + " among the application dependencies");
    }

    private void extractHealthUi(AppArtifact artifact, Path resourceDir) throws IOException {
        for (Path p : artifact.getPaths()) {
            File artifactFile = p.toFile();
            try (JarFile jarFile = new JarFile(artifactFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(HEALTH_UI_WEBJAR_PREFIX) && !entry.isDirectory()) {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            String filename = entry.getName().replace(HEALTH_UI_WEBJAR_PREFIX + "/", "");
                            if (!IGNORE_LIST.contains(filename)) {
                                Files.copy(inputStream, resourceDir.resolve(filename));
                            }
                        }
                    }
                }
                // Now add our own logo and favicon
                ClassLoader classLoader = SmallRyeHealthProcessor.class.getClassLoader();
                for (String ownMedia : IGNORE_LIST) {
                    try (InputStream logo = classLoader.getResourceAsStream(OWN_MEDIA_FOLDER + ownMedia)) {
                        Files.copy(logo, resourceDir.resolve(ownMedia));
                    }
                }
            }
        }
    }

    private void updateApiUrl(Path healthUiJs, String healthPath) throws IOException {
        String content = new String(Files.readAllBytes(healthUiJs), StandardCharsets.UTF_8);
        String result = updateApiUrl(content, healthPath);
        if (result != null) {
            Files.write(healthUiJs, result.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String updateApiUrl(String original, String healthPath) {
        return original.replace("url = \"/health\";", "url = \"" + healthPath + "\";");
    }

    private static final class CachedHealthUI implements Runnable {

        String cachedHealthPath;
        String cachedDirectory;

        @Override
        public void run() {
            try {
                FileUtil.deleteDirectory(Paths.get(cachedDirectory));
            } catch (IOException e) {
                LOG.error("Failed to clean Health UI temp directory on shutdown", e);
            }
        }
    }
}
