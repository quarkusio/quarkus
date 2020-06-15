package io.quarkus.smallrye.graphql.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLRecorder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.smallrye.graphql.cdi.config.ConfigKey;
import io.smallrye.graphql.cdi.config.GraphQLConfig;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.schema.Annotations;
import io.smallrye.graphql.schema.SchemaBuilder;
import io.smallrye.graphql.schema.model.Argument;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.schema.model.InputType;
import io.smallrye.graphql.schema.model.InterfaceType;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Schema;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.spi.LookupService;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Processor for SmallRye GraphQL.
 * We scan all annotations and build the model during build.
 */
public class SmallRyeGraphQLProcessor {
    private static final Logger log = Logger.getLogger(SmallRyeGraphQLProcessor.class);

    private static final Logger LOG = Logger.getLogger(SmallRyeGraphQLProcessor.class.getName());
    private static final String SCHEMA_PATH = "/schema.graphql";
    private static final String SPI_PATH = "META-INF/services/";

    // For the UI
    private static final String GRAPHQL_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String GRAPHQL_UI_WEBJAR_ARTIFACT_ID = "smallrye-graphql-ui-graphiql";
    private static final String GRAPHQL_UI_WEBJAR_PREFIX = "META-INF/resources/graphql-ui";
    private static final String OWN_MEDIA_FOLDER = "META-INF/resources/";
    private static final String GRAPHQL_UI_FINAL_DESTINATION = "META-INF/graphql-ui-files";
    private static final String TEMP_DIR_PREFIX = "quarkus-graphql-ui_" + System.nanoTime();
    private static final List<String> IGNORE_LIST = Arrays.asList("logo.png", "favicon.ico");
    private static final String FILE_TO_UPDATE = "render.js";

    SmallRyeGraphQLConfig quarkusConfig;

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.SMALLRYE_GRAPHQL));
    }

    @BuildStep
    void additionalBeanDefiningAnnotation(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotationProducer) {
        // Make ArC discover the beans marked with the @GraphQlApi qualifier
        beanDefiningAnnotationProducer.produce(new BeanDefiningAnnotationBuildItem(Annotations.GRAPHQL_API));
    }

    @BuildStep
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(GraphQLConfig.class)
                .addBeanClass(GraphQLProducer.class)
                .setUnremovable().build());
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("com.graphql-java", "graphql-java"));
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> serviceProvider) throws IOException {
        // Lookup Service (We use the one from the CDI Module)
        String lookupService = SPI_PATH + LookupService.class.getName();
        Set<String> lookupImplementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                lookupService);
        serviceProvider.produce(
                new ServiceProviderBuildItem(LookupService.class.getName(), lookupImplementations.toArray(new String[0])));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void buildExecutionService(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyProducer,
            SmallRyeGraphQLRecorder recorder,
            BeanContainerBuildItem beanContainer,
            CombinedIndexBuildItem combinedIndex) {

        IndexView index = combinedIndex.getIndex();
        Schema schema = SchemaBuilder.build(index);

        recorder.createExecutionService(beanContainer.getValue(), schema);

        // Make sure the complex object from the application can work in native mode
        for (String c : getClassesToRegisterForReflection(schema)) {
            DotName name = DotName.createSimple(c);
            org.jboss.jandex.Type type = org.jboss.jandex.Type.create(name, org.jboss.jandex.Type.Kind.CLASS);
            reflectiveHierarchyProducer.produce(new ReflectiveHierarchyBuildItem(type, index));
        }

        // Make sure the GraphQL Java classes needed for introspection can work in native mode
        reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, getGraphQLJavaClasses()));
    }

    @BuildStep
    void activateMetrics(Capabilities capabilities,
            SmallRyeGraphQLConfig smallRyeGraphQLConfig,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {
        if (smallRyeGraphQLConfig.metricsEnabled) {
            if (capabilities.isPresent(Capability.METRICS)) {
                unremovableBeans.produce(new UnremovableBeanBuildItem(
                        new UnremovableBeanBuildItem.BeanClassNameExclusion("io.smallrye.metrics.MetricsRegistryImpl")));
                unremovableBeans.produce(new UnremovableBeanBuildItem(
                        new UnremovableBeanBuildItem.BeanClassNameExclusion("io.smallrye.metrics.MetricRegistries")));
                systemProperties.produce(new SystemPropertyBuildItem("smallrye.graphql.metrics.enabled", "true"));
            } else {
                log.warn("The quarkus.smallrye-graphql.metrics.enabled property is true, but the quarkus-smallrye-metrics " +
                        "dependency is not present.");
                systemProperties.produce(new SystemPropertyBuildItem("smallrye.graphql.metrics.enabled", "false"));
            }
        } else {
            systemProperties.produce(new SystemPropertyBuildItem("smallrye.graphql.metrics.enabled", "false"));
        }
    }

    @BuildStep
    void requireBody(BuildProducer<RequireBodyHandlerBuildItem> requireBodyHandlerProducer) {
        // Because we need to read the body
        requireBodyHandlerProducer.produce(new RequireBodyHandlerBuildItem());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void buildEndpoints(
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageDisplayableEndpointProducer,
            LaunchModeBuildItem launchMode,
            SmallRyeGraphQLRecorder recorder,
            ShutdownContextBuildItem shutdownContext,
            BeanContainerBuildItem beanContainerBuildItem // don't remove this - makes sure beanContainer is initialized
    ) {

        /*
         * <em>Ugly Hack</em>
         * In dev mode, we pass a classloader to use in the CDI Loader.
         * This hack is required because using the TCCL would get an outdated version - the initial one.
         * This is because the worker thread on which the handler is called captures the TCCL at creation time
         * and does not allow updating it.
         *
         * In non dev mode, the TCCL is used.
         */
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            recorder.setupClDevMode(shutdownContext);
        }
        // add graphql endpoint for not found display in dev or test mode
        if (launchMode.getLaunchMode().isDevOrTest()) {
            notFoundPageDisplayableEndpointProducer
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(quarkusConfig.rootPath));
            notFoundPageDisplayableEndpointProducer
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(quarkusConfig.rootPath + SCHEMA_PATH));
        }

        Boolean allowGet = ConfigProvider.getConfig().getOptionalValue(ConfigKey.ALLOW_GET, boolean.class).orElse(false);

        Handler<RoutingContext> executionHandler = recorder.executionHandler(allowGet);
        routeProducer.produce(new RouteBuildItem(quarkusConfig.rootPath, executionHandler, HandlerType.BLOCKING));

        Handler<RoutingContext> schemaHandler = recorder.schemaHandler();
        routeProducer.produce(
                new RouteBuildItem(quarkusConfig.rootPath + SCHEMA_PATH, schemaHandler, HandlerType.BLOCKING));
    }

    @BuildStep
    void openTracingIntegration(Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> properties) {
        if (capabilities.isPresent(Capability.SMALLRYE_OPENTRACING)) {
            properties.produce(new SystemPropertyBuildItem("smallrye.graphql.tracing.enabled", "true"));
        }
    }

    private Set<String> getClassesToRegisterForReflection(Schema schema) {
        // Unique list of classes we need to do reflection on
        Set<String> classes = new HashSet<>();

        classes.addAll(getOperationClassNames(schema.getQueries()));
        classes.addAll(getOperationClassNames(schema.getMutations()));
        classes.addAll(getTypeClassNames(schema.getTypes().values()));
        classes.addAll(getInputClassNames(schema.getInputs().values()));
        classes.addAll(getInterfaceClassNames(schema.getInterfaces().values()));

        return classes;
    }

    private Class[] getGraphQLJavaClasses() {
        Set<Class> classes = new HashSet<>();
        classes.add(graphql.schema.FieldCoordinates.class);
        classes.add(graphql.schema.GraphQLArgument.class);
        classes.add(graphql.schema.GraphQLCodeRegistry.class);
        classes.add(graphql.schema.GraphQLEnumType.class);
        classes.add(graphql.schema.GraphQLFieldDefinition.class);
        classes.add(graphql.schema.GraphQLInputObjectField.class);
        classes.add(graphql.schema.GraphQLInputObjectType.class);
        classes.add(graphql.schema.GraphQLInputType.class);
        classes.add(graphql.schema.GraphQLInterfaceType.class);
        classes.add(graphql.schema.GraphQLList.class);
        classes.add(graphql.schema.GraphQLNonNull.class);
        classes.add(graphql.schema.GraphQLObjectType.class);
        classes.add(graphql.schema.GraphQLOutputType.class);
        classes.add(graphql.schema.GraphQLScalarType.class);
        classes.add(graphql.schema.GraphQLSchema.class);
        classes.add(graphql.schema.GraphQLTypeReference.class);
        return classes.toArray(new Class[] {});
    }

    private Set<String> getOperationClassNames(Set<Operation> operations) {
        Set<String> classes = new HashSet<>();
        for (Operation operation : operations) {
            classes.add(operation.getClassName());
            for (Argument argument : operation.getArguments()) {
                classes.add(argument.getReference().getClassName());
            }
            classes.add(operation.getReference().getClassName());
        }
        return classes;
    }

    private Set<String> getTypeClassNames(Collection<Type> complexGraphQLTypes) {
        Set<String> classes = new HashSet<>();
        for (Type complexGraphQLType : complexGraphQLTypes) {
            classes.add(complexGraphQLType.getClassName());
            classes.addAll(getFieldClassNames(complexGraphQLType.getFields()));
        }
        return classes;
    }

    private Set<String> getInputClassNames(Collection<InputType> complexGraphQLTypes) {
        Set<String> classes = new HashSet<>();
        for (InputType complexGraphQLType : complexGraphQLTypes) {
            classes.add(complexGraphQLType.getClassName());
            classes.addAll(getFieldClassNames(complexGraphQLType.getFields()));
        }
        return classes;
    }

    private Set<String> getInterfaceClassNames(Collection<InterfaceType> complexGraphQLTypes) {
        Set<String> classes = new HashSet<>();
        for (InterfaceType complexGraphQLType : complexGraphQLTypes) {
            classes.add(complexGraphQLType.getClassName());
            classes.addAll(getFieldClassNames(complexGraphQLType.getFields()));
        }
        return classes;
    }

    private Set<String> getFieldClassNames(Set<Field> fields) {
        Set<String> classes = new HashSet<>();
        for (Field field : fields) {
            classes.add(field.getReference().getClassName());
        }
        return classes;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerGraphQLUiServletExtension(
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageDisplayableEndpointProducer,
            SmallRyeGraphQLRecorder recorder,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReload,
            HttpRootPathBuildItem httpRootPath,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {

        if (!quarkusConfig.enableUi) {
            return;
        }
        if ("/".equals(quarkusConfig.rootPathUi)) {
            throw new ConfigurationError(
                    "quarkus.smallrye-graphql.root-path-ui was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
        }

        String graphQLPath = httpRootPath.adjustPath(quarkusConfig.rootPath);

        if (launchMode.getLaunchMode().isDevOrTest()) {
            CachedGraphQLUI cached = liveReload.getContextObject(CachedGraphQLUI.class);
            boolean extractionNeeded = cached == null;

            if (cached != null && !cached.cachedGraphQLPath.equals(graphQLPath)) {
                try {
                    FileUtil.deleteDirectory(Paths.get(cached.cachedDirectory));
                } catch (IOException e) {
                    LOG.error("Failed to clean GraphQL UI temp directory on restart", e);
                }
                extractionNeeded = true;
            }
            if (extractionNeeded) {
                if (cached == null) {
                    cached = new CachedGraphQLUI();
                    liveReload.setContextObject(CachedGraphQLUI.class, cached);
                    Runtime.getRuntime().addShutdownHook(new Thread(cached, "GraphQL UI Shutdown Hook"));
                }
                try {
                    AppArtifact artifact = getGraphQLUiArtifact(curateOutcomeBuildItem);
                    Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX).toRealPath();
                    extractGraphQLUi(artifact, tempDir);
                    updateApiUrl(tempDir.resolve(FILE_TO_UPDATE), graphQLPath);
                    cached.cachedDirectory = tempDir.toAbsolutePath().toString();
                    cached.cachedGraphQLPath = graphQLPath;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Handler<RoutingContext> handler = recorder.uiHandler(cached.cachedDirectory,
                    httpRootPath.adjustPath(quarkusConfig.rootPathUi));
            routeProducer.produce(new RouteBuildItem(quarkusConfig.rootPathUi, handler));
            routeProducer.produce(new RouteBuildItem(quarkusConfig.rootPathUi + "/*", handler));
            notFoundPageDisplayableEndpointProducer
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(quarkusConfig.rootPathUi + "/"));
        } else if (quarkusConfig.alwaysIncludeUi) {
            AppArtifact artifact = getGraphQLUiArtifact(curateOutcomeBuildItem);
            //we are including in a production artifact
            //just stick the files in the generated output
            //we could do this for dev mode as well but then we need to extract them every time
            for (Path p : artifact.getPaths()) {
                File artifactFile = p.toFile();
                try (JarFile jarFile = new JarFile(artifactFile)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(GRAPHQL_UI_WEBJAR_PREFIX) && !entry.isDirectory()) {
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                String filename = entry.getName().replace(GRAPHQL_UI_WEBJAR_PREFIX + "/", "");
                                byte[] content = FileUtil.readFileContents(inputStream);
                                if (entry.getName().endsWith(FILE_TO_UPDATE)) {
                                    content = updateApiUrl(new String(content, StandardCharsets.UTF_8), graphQLPath)
                                            .getBytes(StandardCharsets.UTF_8);
                                }
                                if (IGNORE_LIST.contains(filename)) {
                                    ClassLoader classLoader = SmallRyeGraphQLProcessor.class.getClassLoader();
                                    try (InputStream resourceAsStream = classLoader
                                            .getResourceAsStream(OWN_MEDIA_FOLDER + filename)) {
                                        content = IoUtil.readBytes(resourceAsStream);
                                    }
                                }

                                String fileName = GRAPHQL_UI_FINAL_DESTINATION + "/" + filename;

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
                    .uiHandler(GRAPHQL_UI_FINAL_DESTINATION, httpRootPath.adjustPath(quarkusConfig.rootPathUi));
            routeProducer.produce(new RouteBuildItem(quarkusConfig.rootPathUi, handler));
            routeProducer.produce(new RouteBuildItem(quarkusConfig.rootPathUi + "/*", handler));
        }
    }

    private AppArtifact getGraphQLUiArtifact(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        for (AppDependency dep : curateOutcomeBuildItem.getEffectiveModel().getFullDeploymentDeps()) {
            if (dep.getArtifact().getArtifactId().equals(GRAPHQL_UI_WEBJAR_ARTIFACT_ID)
                    && dep.getArtifact().getGroupId().equals(GRAPHQL_UI_WEBJAR_GROUP_ID)) {
                return dep.getArtifact();
            }
        }
        throw new RuntimeException("Could not find artifact " + GRAPHQL_UI_WEBJAR_GROUP_ID + ":" + GRAPHQL_UI_WEBJAR_ARTIFACT_ID
                + " among the application dependencies");
    }

    private void extractGraphQLUi(AppArtifact artifact, Path resourceDir) throws IOException {
        for (Path p : artifact.getPaths()) {
            File artifactFile = p.toFile();
            try (JarFile jarFile = new JarFile(artifactFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(GRAPHQL_UI_WEBJAR_PREFIX) && !entry.isDirectory()) {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            String filename = entry.getName().replace(GRAPHQL_UI_WEBJAR_PREFIX + "/", "");
                            if (!IGNORE_LIST.contains(filename)) {
                                Files.copy(inputStream, resourceDir.resolve(filename));
                            }
                        }
                    }
                }
                // Now add our own logo and favicon
                ClassLoader classLoader = SmallRyeGraphQLProcessor.class.getClassLoader();
                for (String ownMedia : IGNORE_LIST) {
                    try (InputStream logo = classLoader.getResourceAsStream(OWN_MEDIA_FOLDER + ownMedia)) {
                        Files.copy(logo, resourceDir.resolve(ownMedia));
                    }
                }
            }
        }
    }

    private void updateApiUrl(Path renderJs, String graphqlPath) throws IOException {
        String content = new String(Files.readAllBytes(renderJs), StandardCharsets.UTF_8);
        String result = updateApiUrl(content, graphqlPath);
        if (result != null) {
            Files.write(renderJs, result.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String updateApiUrl(String original, String graphqlPath) {
        return original.replace("const api = '/graphql';", "const api = '" + graphqlPath + "';");
    }

    private static final class CachedGraphQLUI implements Runnable {

        String cachedGraphQLPath;
        String cachedDirectory;

        @Override
        public void run() {
            try {
                FileUtil.deleteDirectory(Paths.get(cachedDirectory));
            } catch (IOException e) {
                LOG.error("Failed to clean GraphQL UI temp directory on shutdown", e);
            }
        }
    }
}
