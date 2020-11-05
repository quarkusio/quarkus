package io.quarkus.smallrye.graphql.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.model.AppArtifact;
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
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.deployment.util.WebJarUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLRecorder;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLRuntimeConfig;
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
import io.smallrye.graphql.schema.model.Group;
import io.smallrye.graphql.schema.model.InputType;
import io.smallrye.graphql.schema.model.InterfaceType;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Reference;
import io.smallrye.graphql.schema.model.Schema;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.spi.EventingService;
import io.smallrye.graphql.spi.LookupService;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Processor for SmallRye GraphQL.
 * We scan all annotations and build the model during build.
 */
public class SmallRyeGraphQLProcessor {
    private static final Logger LOG = Logger.getLogger(SmallRyeGraphQLProcessor.class);
    private static final String SCHEMA_PATH = "/schema.graphql";
    private static final String SPI_PATH = "META-INF/services/";

    // For Service integration
    private static final String SERVICE_NOT_AVAILABLE_WARNING = "The %s property is true, but the %s extension is not present. SmallRye GraphQL %s will be disabled.";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    // For the UI
    private static final String GRAPHQL_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String GRAPHQL_UI_WEBJAR_ARTIFACT_ID = "smallrye-graphql-ui-graphiql";
    private static final String GRAPHQL_UI_WEBJAR_PREFIX = "META-INF/resources/graphql-ui/";
    private static final String GRAPHQL_UI_FINAL_DESTINATION = "META-INF/graphql-ui-files";
    private static final String FILE_TO_UPDATE = "render.js";
    private static final String LINE_TO_UPDATE = "const api = '";
    private static final String LINE_FORMAT = LINE_TO_UPDATE + "%s';";

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

        // Eventing Service (We use the one from the CDI Module)
        String eventingService = SPI_PATH + EventingService.class.getName();
        Set<String> eventingServiceImplementations = ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                eventingService);
        for (String eventingServiceImplementation : eventingServiceImplementations) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem(EventingService.class.getName(), eventingServiceImplementation));
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void buildExecutionService(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyProducer,
            SmallRyeGraphQLRecorder recorder,
            BeanContainerBuildItem beanContainer,
            CombinedIndexBuildItem combinedIndex,
            SmallRyeGraphQLConfig graphQLConfig) {

        IndexView index = combinedIndex.getIndex();

        Schema schema = SchemaBuilder.build(index, graphQLConfig.autoNameStrategy);

        recorder.createExecutionService(beanContainer.getValue(), schema);

        // Make sure the complex object from the application can work in native mode
        reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, getSchemaJavaClasses(schema)));

        // Make sure the GraphQL Java classes needed for introspection can work in native mode
        reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, getGraphQLJavaClasses()));
    }

    @BuildStep
    void requireBody(BuildProducer<RequireBodyHandlerBuildItem> requireBodyHandlerProducer) {
        // Because we need to read the body
        requireBodyHandlerProducer.produce(new RequireBodyHandlerBuildItem());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void buildSchemaEndpoint(
            BuildProducer<RouteBuildItem> routeProducer,
            SmallRyeGraphQLRecorder recorder,
            SmallRyeGraphQLConfig graphQLConfig) {

        Handler<RoutingContext> schemaHandler = recorder.schemaHandler();
        routeProducer.produce(
                new RouteBuildItem(graphQLConfig.rootPath + SCHEMA_PATH, schemaHandler, HandlerType.BLOCKING));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void buildExecutionEndpoint(
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageDisplayableEndpointProducer,
            SmallRyeGraphQLRecorder recorder,
            ShutdownContextBuildItem shutdownContext,
            LaunchModeBuildItem launchMode,
            SmallRyeGraphQLConfig graphQLConfig,
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
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(graphQLConfig.rootPath));
            notFoundPageDisplayableEndpointProducer
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(graphQLConfig.rootPath + SCHEMA_PATH));
        }

        Boolean allowGet = ConfigProvider.getConfig().getOptionalValue(ConfigKey.ALLOW_GET, boolean.class).orElse(false);

        Handler<RoutingContext> executionHandler = recorder.executionHandler(allowGet);
        routeProducer.produce(new RouteBuildItem(graphQLConfig.rootPath, executionHandler, HandlerType.BLOCKING));

    }

    private String[] getSchemaJavaClasses(Schema schema) {
        // Unique list of classes we need to do reflection on
        Set<String> classes = new HashSet<>();

        classes.addAll(getOperationClassNames(schema.getQueries()));
        classes.addAll(getOperationClassNames(schema.getGroupedQueries()));
        classes.addAll(getOperationClassNames(schema.getMutations()));
        classes.addAll(getOperationClassNames(schema.getGroupedMutations()));
        classes.addAll(getTypeClassNames(schema.getTypes().values()));
        classes.addAll(getInputClassNames(schema.getInputs().values()));
        classes.addAll(getInterfaceClassNames(schema.getInterfaces().values()));

        return classes.toArray(new String[] {});
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
        classes.add(List.class);
        return classes.toArray(new Class[] {});
    }

    private Set<String> getOperationClassNames(Set<Operation> operations) {
        Set<String> classes = new HashSet<>();
        for (Operation operation : operations) {
            classes.add(operation.getClassName());
            for (Argument argument : operation.getArguments()) {
                classes.addAll(getAllReferenceClasses(argument.getReference()));
            }
            classes.addAll(getAllReferenceClasses(operation.getReference()));
        }
        return classes;
    }

    private Set<String> getOperationClassNames(Map<Group, Set<Operation>> groupedOperations) {
        Set<String> classes = new HashSet<>();
        Collection<Set<Operation>> operations = groupedOperations.values();
        for (Set<Operation> operationSet : operations) {
            classes.addAll(getOperationClassNames(operationSet));
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

    private Set<String> getFieldClassNames(Map<String, Field> fields) {
        Set<String> classes = new HashSet<>();
        for (Field field : fields.values()) {
            classes.addAll(getAllReferenceClasses(field.getReference()));
        }
        return classes;
    }

    private Set<String> getAllReferenceClasses(Reference reference) {
        Set<String> classes = new HashSet<>();
        classes.add(reference.getClassName());
        if (reference.getParametrizedTypeArguments() != null && !reference.getParametrizedTypeArguments().isEmpty()) {

            Collection<Reference> parametrized = reference.getParametrizedTypeArguments().values();
            for (Reference r : parametrized) {
                classes.addAll(getAllReferenceClasses(r));
            }
        }
        return classes;
    }

    // Services Integrations

    @BuildStep
    void activateMetrics(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        boolean activate = shouldActivateService(capabilities,
                graphQLConfig.metricsEnabled,
                metricsCapability.isPresent(),
                "quarkus-smallrye-metrics",
                "metrics",
                "quarkus.smallrye-graphql.metrics.enabled");
        if (activate) {
            if (metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MetricsFactory.MP_METRICS)) {
                unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames("io.smallrye.metrics.MetricRegistries"));
            }
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_METRICS, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_METRICS, FALSE));
        }
    }

    @BuildStep
    void activateTracing(Capabilities capabilities,
            SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {

        boolean activate = shouldActivateService(capabilities,
                graphQLConfig.tracingEnabled,
                "quarkus-smallrye-opentracing",
                Capability.OPENTRACING,
                "quarkus.smallrye-graphql.tracing.enabled");
        if (activate) {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_TRACING, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_TRACING, FALSE));
        }
    }

    @BuildStep
    void activateValidation(Capabilities capabilities,
            SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {

        boolean activate = shouldActivateService(capabilities,
                graphQLConfig.validationEnabled,
                "quarkus-hibernate-validator",
                Capability.HIBERNATE_VALIDATOR,
                "quarkus.smallrye-graphql.validation.enabled");
        if (activate) {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_VALIDATION, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_VALIDATION, FALSE));
        }
    }

    @BuildStep
    void activateEventing(SmallRyeGraphQLConfig graphQLConfig, BuildProducer<SystemPropertyBuildItem> systemProperties) {
        if (graphQLConfig.eventsEnabled) {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_EVENTS, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_EVENTS, FALSE));
        }
    }

    private boolean shouldActivateService(Capabilities capabilities,
            Optional<Boolean> serviceEnabled,
            String linkedExtensionName,
            Capability linkedCapability,
            String configKey) {

        return shouldActivateService(capabilities, serviceEnabled, capabilities.isPresent(linkedCapability),
                linkedExtensionName, linkedCapability.getName(), configKey);
    }

    private boolean shouldActivateService(Capabilities capabilities,
            Optional<Boolean> serviceEnabled,
            boolean linkedCapabilityIsPresent,
            String linkedExtensionName,
            String linkedCapabilityName,
            String configKey) {

        if (serviceEnabled.isPresent()) {
            // The user explicitly asked from something
            boolean isEnabled = serviceEnabled.get();
            if (isEnabled && !linkedCapabilityIsPresent) {
                // Warn and disable
                LOG.warnf(SERVICE_NOT_AVAILABLE_WARNING, configKey, linkedExtensionName, linkedCapabilityName);
            }
            return (isEnabled && linkedCapabilityIsPresent);
        } else {
            // Auto dis/enable
            return linkedCapabilityIsPresent;
        }
    }

    // UI Related

    @BuildStep
    void getGraphqlUiFinalDestination(
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageDisplayableEndpointProducer,
            BuildProducer<SmallRyeGraphQLBuildItem> smallRyeGraphQLBuildProducer,
            HttpRootPathBuildItem httpRootPath,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeGraphQLConfig graphQLConfig) throws Exception {

        if (shouldInclude(launchMode, graphQLConfig)) {

            if ("/".equals(graphQLConfig.ui.rootPath)) {
                throw new ConfigurationError(
                        "quarkus.smallrye-graphql.root-path-ui was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
            }

            String graphQLPath = httpRootPath.adjustPath(graphQLConfig.rootPath);

            AppArtifact artifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, GRAPHQL_UI_WEBJAR_GROUP_ID,
                    GRAPHQL_UI_WEBJAR_ARTIFACT_ID);
            if (launchMode.getLaunchMode().isDevOrTest()) {
                Path tempPath = WebJarUtil.devOrTest(curateOutcomeBuildItem, launchMode, artifact, GRAPHQL_UI_WEBJAR_PREFIX);
                WebJarUtil.updateUrl(tempPath.resolve(FILE_TO_UPDATE), graphQLPath, LINE_TO_UPDATE, LINE_FORMAT);

                smallRyeGraphQLBuildProducer.produce(new SmallRyeGraphQLBuildItem(tempPath.toAbsolutePath().toString(),
                        httpRootPath.adjustPath(graphQLConfig.ui.rootPath)));
                notFoundPageDisplayableEndpointProducer
                        .produce(new NotFoundPageDisplayableEndpointBuildItem(graphQLConfig.ui.rootPath + "/"));

            } else {
                Map<String, byte[]> files = WebJarUtil.production(curateOutcomeBuildItem, artifact, GRAPHQL_UI_WEBJAR_PREFIX);

                for (Map.Entry<String, byte[]> file : files.entrySet()) {

                    String fileName = file.getKey();
                    byte[] content = file.getValue();
                    if (fileName.endsWith(FILE_TO_UPDATE)) {
                        content = WebJarUtil
                                .updateUrl(new String(content, StandardCharsets.UTF_8), graphQLPath, LINE_TO_UPDATE,
                                        LINE_FORMAT)
                                .getBytes(StandardCharsets.UTF_8);
                    }
                    fileName = GRAPHQL_UI_FINAL_DESTINATION + "/" + fileName;

                    generatedResourceProducer.produce(new GeneratedResourceBuildItem(fileName, content));
                    nativeImageResourceProducer.produce(new NativeImageResourceBuildItem(fileName));
                }

                smallRyeGraphQLBuildProducer.produce(new SmallRyeGraphQLBuildItem(GRAPHQL_UI_FINAL_DESTINATION,
                        httpRootPath.adjustPath(graphQLConfig.ui.rootPath)));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerGraphQLUiHandler(
            BuildProducer<RouteBuildItem> routeProducer,
            SmallRyeGraphQLRecorder recorder,
            SmallRyeGraphQLRuntimeConfig runtimeConfig,
            SmallRyeGraphQLBuildItem smallRyeGraphQLBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeGraphQLConfig graphQLConfig) throws Exception {

        if (shouldInclude(launchMode, graphQLConfig)) {
            Handler<RoutingContext> handler = recorder.uiHandler(smallRyeGraphQLBuildItem.getGraphqlUiFinalDestination(),
                    smallRyeGraphQLBuildItem.getGraphqlUiPath(), runtimeConfig);
            routeProducer.produce(new RouteBuildItem(graphQLConfig.ui.rootPath, handler));
            routeProducer.produce(new RouteBuildItem(graphQLConfig.ui.rootPath + "/*", handler));
        }
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, SmallRyeGraphQLConfig graphQLConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || graphQLConfig.ui.alwaysInclude;
    }
}
