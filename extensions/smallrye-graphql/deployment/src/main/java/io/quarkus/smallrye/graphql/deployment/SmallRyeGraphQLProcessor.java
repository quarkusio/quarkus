package io.quarkus.smallrye.graphql.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.KnownCompatibleBeanArchiveBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.devui.spi.buildtime.FooterLogBuildItem;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.graphql.runtime.ExtraScalar;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLConfig;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLConfigMapping;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLLocaleResolver;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLRecorder;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.quarkus.vertx.http.deployment.spi.WebDependencyJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResourcesFilter;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.smallrye.config.Converters;
import io.smallrye.graphql.api.AdaptWith;
import io.smallrye.graphql.api.Deprecated;
import io.smallrye.graphql.api.Entry;
import io.smallrye.graphql.api.ErrorExtensionProvider;
import io.smallrye.graphql.api.Namespace;
import io.smallrye.graphql.api.OneOf;
import io.smallrye.graphql.api.federation.Authenticated;
import io.smallrye.graphql.api.federation.ComposeDirective;
import io.smallrye.graphql.api.federation.Extends;
import io.smallrye.graphql.api.federation.External;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Inaccessible;
import io.smallrye.graphql.api.federation.InterfaceObject;
import io.smallrye.graphql.api.federation.Key;
import io.smallrye.graphql.api.federation.Provides;
import io.smallrye.graphql.api.federation.Requires;
import io.smallrye.graphql.api.federation.Resolver;
import io.smallrye.graphql.api.federation.Shareable;
import io.smallrye.graphql.api.federation.Tag;
import io.smallrye.graphql.api.federation.link.Import;
import io.smallrye.graphql.api.federation.link.Link;
import io.smallrye.graphql.api.federation.link.Purpose;
import io.smallrye.graphql.api.federation.policy.Policy;
import io.smallrye.graphql.api.federation.policy.PolicyGroup;
import io.smallrye.graphql.api.federation.policy.PolicyItem;
import io.smallrye.graphql.api.federation.requiresscopes.RequiresScopes;
import io.smallrye.graphql.api.federation.requiresscopes.ScopeGroup;
import io.smallrye.graphql.api.federation.requiresscopes.ScopeItem;
import io.smallrye.graphql.cdi.config.MicroProfileConfig;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.cdi.tracing.TracingService;
import io.smallrye.graphql.config.ConfigKey;
import io.smallrye.graphql.schema.Annotations;
import io.smallrye.graphql.schema.SchemaBuilder;
import io.smallrye.graphql.schema.helper.TypeAutoNameStrategy;
import io.smallrye.graphql.schema.model.Argument;
import io.smallrye.graphql.schema.model.DirectiveType;
import io.smallrye.graphql.schema.model.EnumType;
import io.smallrye.graphql.schema.model.EnumValue;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.schema.model.InputType;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Reference;
import io.smallrye.graphql.schema.model.ReferenceType;
import io.smallrye.graphql.schema.model.Scalars;
import io.smallrye.graphql.schema.model.Schema;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.schema.model.UnionType;
import io.smallrye.graphql.schema.model.Wrapper;
import io.smallrye.graphql.spi.EventingService;
import io.smallrye.graphql.spi.LookupService;
import io.smallrye.graphql.spi.config.Config;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Processor for SmallRye GraphQL.
 * We scan all annotations and build the model during build.
 */
public class SmallRyeGraphQLProcessor {
    private static final Logger LOG = Logger.getLogger(SmallRyeGraphQLProcessor.class);
    private static final String SCHEMA_PATH = "schema.graphql";

    // For Service integration
    private static final String SERVICE_NOT_AVAILABLE_WARNING = "The %s property is true, but the %s extension is not present. SmallRye GraphQL %s will be disabled.";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    // For the UI
    private static final GACT GRAPHQL_UI_WEBJAR_ARTIFACT_KEY = new GACT("io.smallrye", "smallrye-graphql-ui-graphiql", null,
            "jar");
    private static final String GRAPHQL_UI_WEBJAR_STATIC_RESOURCES_PATH = "META-INF/resources/graphql-ui/";
    private static final String FILE_TO_UPDATE = "render.js";
    private static final String LINE_TO_UPDATE = "const api = '";
    private static final String LINE_FORMAT = LINE_TO_UPDATE + "%s';";
    private static final String UI_LINE_TO_UPDATE = "const ui = '";
    private static final String UI_LINE_FORMAT = UI_LINE_TO_UPDATE + "%s';";
    private static final String LOGO_LINE_TO_UPDATE = "const logo = '";
    private static final String LOGO_LINE_FORMAT = LOGO_LINE_TO_UPDATE + "%s';";

    // Branding files to monitor for changes
    private static final String BRANDING_DIR = "META-INF/branding/";
    private static final String BRANDING_LOGO_GENERAL = BRANDING_DIR + "logo.png";
    private static final String BRANDING_LOGO_MODULE = BRANDING_DIR + "smallrye-graphql-ui-graphiql.png";
    private static final String BRANDING_STYLE_GENERAL = BRANDING_DIR + "style.css";
    private static final String BRANDING_STYLE_MODULE = BRANDING_DIR + "smallrye-graphql-ui-graphiql.css";
    private static final String BRANDING_FAVICON_GENERAL = BRANDING_DIR + "favicon.ico";
    private static final String BRANDING_FAVICON_MODULE = BRANDING_DIR + "smallrye-graphql-ui-graphiql.ico";

    private static final String SUBPROTOCOL_GRAPHQL_WS = "graphql-ws";
    private static final String SUBPROTOCOL_GRAPHQL_TRANSPORT_WS = "graphql-transport-ws";
    private static final List<String> SUPPORTED_WEBSOCKET_SUBPROTOCOLS = List.of(SUBPROTOCOL_GRAPHQL_WS,
            SUBPROTOCOL_GRAPHQL_TRANSPORT_WS);

    private static final int GRAPHQL_WEBSOCKET_HANDLER_ORDER = (-1 * SecurityHandlerPriorities.AUTHORIZATION) + 1;

    private static final String GRAPHQL_MEDIA_TYPE = "application/graphql+json";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.SMALLRYE_GRAPHQL));
    }

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
    void additionalBeanDefiningAnnotation(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotationProducer) {
        // Make ArC discover the beans marked with the @GraphQLApi qualifier
        beanDefiningAnnotationProducer
                .produce(new BeanDefiningAnnotationBuildItem(Annotations.GRAPHQL_API, BuiltinScope.SINGLETON.getName()));
    }

    @BuildStep
    void autoTransactional(Capabilities capabilities,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        if (!capabilities.isPresent(Capability.TRANSACTIONS)) {
            return;
        }

        // Don't add @Transactional when Hibernate Reactive is present — it would
        // interfere with reactive session management
        if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
            return;
        }

        DotName transactional = DotName.createSimple("jakarta.transaction.Transactional");

        Set<DotName> reactiveTypes = Set.of(
                DotName.createSimple("io.smallrye.mutiny.Uni"),
                DotName.createSimple("io.smallrye.mutiny.Multi"),
                DotName.createSimple("java.util.concurrent.CompletionStage"),
                DotName.createSimple("java.util.concurrent.CompletableFuture"),
                DotName.createSimple("org.reactivestreams.Publisher"));

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                var method = ctx.getTarget().asMethod();

                if (!method.declaringClass().hasAnnotation(Annotations.GRAPHQL_API)) {
                    return;
                }

                boolean isGraphQLMethod = method.hasAnnotation(Annotations.QUERY)
                        || method.hasAnnotation(Annotations.MUTATION)
                        || method.hasAnnotation(Annotations.SUBCRIPTION)
                        || method.hasAnnotation(Annotations.NAME)
                        || method.hasAnnotation(Annotations.RESOLVER)
                        || method.hasAnnotation(Annotations.SOURCE);
                if (!isGraphQLMethod) {
                    return;
                }

                if (reactiveTypes.contains(method.returnType().name())) {
                    return;
                }

                if (method.hasAnnotation(transactional)) {
                    return;
                }

                if (method.hasAnnotation(Annotations.NON_BLOCKING)) {
                    return;
                }

                ctx.transform().add(transactional).done();
            }
        }));
    }

    @BuildStep
    void additionalBean(Capabilities capabilities, CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(GraphQLProducer.class)
                .setUnremovable().build());
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(SmallRyeGraphQLLocaleResolver.class)
                    .setUnremovable().build());
        }

        // Make sure the adapters does not get removed
        Set<String> adapterClasses = getAllAdapterClasses(combinedIndex.getIndex());
        for (String adapterClass : adapterClasses) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(adapterClass)
                    .setUnremovable().build());
        }
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("com.graphql-java", "graphql-java"));
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<ReflectiveClassConditionBuildItem> reflectiveClassCondition) throws IOException {
        // Lookup Service (We use the one from the CDI Module)
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(LookupService.class.getName()));

        // Eventing Service (We use the one from the CDI Module)
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(EventingService.class.getName()));

        // Add a condition for the optional eventing services
        reflectiveClassCondition
                .produce(new ReflectiveClassConditionBuildItem(TracingService.class, "io.opentelemetry.api.trace.Tracer"));

        // Use MicroProfile Config (We use the one from the CDI Module)
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(MicroProfileConfig.class.getName()));

        // Config mapping between SmallRye / MP and Quarkus
        serviceProvider
                .produce(ServiceProviderBuildItem.allProvidersFromClassPath(SmallRyeGraphQLConfigMapping.class.getName()));

        // ErrorCode and Exception Name Provider
        serviceProvider
                .produce(ServiceProviderBuildItem.allProvidersFromClassPath(ErrorExtensionProvider.class.getName()));
    }

    @BuildStep
    void registerNativeResourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> nativeResourceBundleProvider)
            throws IOException {
        nativeResourceBundleProvider.produce(new NativeImageResourceBundleBuildItem("i18n.Validation"));
        nativeResourceBundleProvider.produce(new NativeImageResourceBundleBuildItem("i18n.Parsing"));
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("graphql.util.IdGenerator"));
    }

    @BuildStep
    SmallRyeGraphQLModifiedClasesBuildItem createIndex(TransformedClassesBuildItem transformedClassesBuildItem) {
        Map<String, byte[]> modifiedClasses = new HashMap<>();
        Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar = transformedClassesBuildItem
                .getTransformedClassesByJar();
        for (Map.Entry<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJarEntrySet : transformedClassesByJar
                .entrySet()) {

            Set<TransformedClassesBuildItem.TransformedClass> transformedClasses = transformedClassesByJarEntrySet.getValue();
            for (TransformedClassesBuildItem.TransformedClass transformedClass : transformedClasses) {
                modifiedClasses.put(transformedClass.getClassName(), transformedClass.getData());
            }
        }
        return new SmallRyeGraphQLModifiedClasesBuildItem(modifiedClasses);
    }

    @BuildStep
    void buildFinalIndex(
            BuildProducer<SmallRyeGraphQLFinalIndexBuildItem> smallRyeGraphQLFinalIndexProducer,
            CombinedIndexBuildItem combinedIndex,
            SmallRyeGraphQLModifiedClasesBuildItem graphQLIndexBuildItem,
            List<SmallRyeGraphQLFinalIndexModifierBuildItem> indexModifiers) {

        List<Map.Entry<String, byte[]>> list = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : graphQLIndexBuildItem.getModifiedClases().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                list.add(entry);
            }
        }

        // feed classes to the `Indexer` in deterministic order
        list.sort(Map.Entry.comparingByKey());

        Indexer indexer = new Indexer();

        for (Map.Entry<String, byte[]> kv : list) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(kv.getValue())) {
                indexer.index(bais);
            } catch (IOException ex) {
                LOG.warn("Could not index [" + kv.getKey() + "] - " + ex.getMessage());
            }
        }

        try {
            indexer.indexClass(Map.class);
            indexer.indexClass(Entry.class);
            indexer.indexClass(Extends.class);
            indexer.indexClass(External.class);
            indexer.indexClass(Key.class);
            indexer.indexClass(Provides.class);
            indexer.indexClass(Requires.class);
            indexer.indexClass(Deprecated.class);
            indexer.indexClass(Shareable.class);
            indexer.indexClass(ComposeDirective.class);
            indexer.indexClass(InterfaceObject.class);
            indexer.indexClass(Inaccessible.class);
            indexer.indexClass(io.smallrye.graphql.api.federation.Override.class);
            indexer.indexClass(Tag.class);
            indexer.indexClass(OneOf.class);
            indexer.indexClass(Authenticated.class);
            indexer.indexClass(FieldSet.class);
            indexer.indexClass(Link.class);
            indexer.indexClass(Import.class);
            indexer.indexClass(Purpose.class);
            indexer.indexClass(Policy.class);
            indexer.indexClass(PolicyGroup.class);
            indexer.indexClass(PolicyItem.class);
            indexer.indexClass(RequiresScopes.class);
            indexer.indexClass(ScopeGroup.class);
            indexer.indexClass(ScopeItem.class);
            indexer.indexClass(Namespace.class);
            indexer.indexClass(Resolver.class);
        } catch (IOException ex) {
            LOG.warn("Failure while creating index", ex);
        }

        IndexView finalIndex = OverridableIndex.create(combinedIndex.getIndex(), indexer.complete());

        // Apply index modifiers in priority order (lower priority values are applied first)
        if (!indexModifiers.isEmpty()) {
            List<SmallRyeGraphQLFinalIndexModifierBuildItem> sortedModifiers = indexModifiers.stream()
                    .sorted(Comparator.comparingInt(SmallRyeGraphQLFinalIndexModifierBuildItem::priority))
                    .toList();

            for (SmallRyeGraphQLFinalIndexModifierBuildItem modifierItem : sortedModifiers) {
                finalIndex = Objects.requireNonNull(
                        modifierItem.modifier().modify(finalIndex),
                        "Index modifier must not return null");
            }
        }

        smallRyeGraphQLFinalIndexProducer.produce(new SmallRyeGraphQLFinalIndexBuildItem(finalIndex));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    void createDevUILog(BuildProducer<FooterLogBuildItem> footerLogProducer,
            SmallRyeGraphQLRecorder recorder,
            BuildProducer<GraphQLDevUILogBuildItem> graphQLDevUILogProducer) {
        RuntimeValue<SubmissionPublisher<String>> publisher = recorder.createTraficLogPublisher();
        footerLogProducer.produce(new FooterLogBuildItem("GraphQL", publisher));
        graphQLDevUILogProducer.produce(new GraphQLDevUILogBuildItem(publisher));
    }

    @BuildStep
    void buildSchema(
            BuildProducer<SmallRyeGraphQLSchemaBuildItem> schemaBuildItemProducer,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            SmallRyeGraphQLFinalIndexBuildItem graphQLFinalIndexBuildItem,
            SmallRyeGraphQLConfig graphQLConfig) {

        activateFederation(graphQLConfig, systemPropertyProducer, graphQLFinalIndexBuildItem);
        graphQLConfig.extraScalars().ifPresent(this::registerExtraScalarsInSchema);
        Schema schema = SchemaBuilder.build(graphQLFinalIndexBuildItem.getFinalIndex(),
                Converters.getImplicitConverter(TypeAutoNameStrategy.class).convert(graphQLConfig.autoNameStrategy()));
        schemaBuildItemProducer.produce(new SmallRyeGraphQLSchemaBuildItem(schema));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void buildExecutionService(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyProducer,
            BuildProducer<SmallRyeGraphQLInitializedBuildItem> graphQLInitializedProducer,
            SmallRyeGraphQLRecorder recorder,
            SmallRyeGraphQLSchemaBuildItem schemaBuildItem,
            BeanContainerBuildItem beanContainer,
            SmallRyeGraphQLConfig graphQLConfig,
            Optional<GraphQLDevUILogBuildItem> graphQLDevUILogBuildItem) {

        Schema schema = schemaBuildItem.getSchema();

        Optional publisher = Optional.empty();
        if (graphQLDevUILogBuildItem.isPresent()) {
            publisher = Optional.of(graphQLDevUILogBuildItem.get().getPublisher());
        }
        RuntimeValue<Boolean> initialized = recorder.createExecutionService(beanContainer.getValue(), schema, publisher);
        graphQLInitializedProducer.produce(new SmallRyeGraphQLInitializedBuildItem(initialized));

        // Make sure the complex object from the application can work in native mode
        reflectiveClassProducer
                .produce(ReflectiveClassBuildItem.builder(getSchemaJavaClasses(schema))
                        .reason(getClass().getName())
                        .methods().fields().build());

        // Make sure the GraphQL Java classes needed for introspection can work in native mode
        reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(getGraphQLJavaClasses())
                .reason(getClass().getName())
                .methods().fields().build());
    }

    private void registerExtraScalarsInSchema(List<ExtraScalar> extraScalars) {
        for (ExtraScalar extraScalar : extraScalars) {
            switch (extraScalar) {
                case UUID:
                    Scalars.addUuid();
                case OBJECT:
                    Scalars.addObject();
                case JSON:
                    Scalars.addJson();
            }
        }
    }

    @BuildStep
    void generateJsClient(
            SmallRyeGraphQLConfig graphQLConfig,
            SmallRyeGraphQLSchemaBuildItem schemaBuildItem,
            HttpRootPathBuildItem httpRootPathBuildItem,
            BuildProducer<GeneratedStaticResourceBuildItem> staticResourceProducer) {

        if (!graphQLConfig.jsClient().enabled()) {
            return;
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = tccl.getResourceAsStream("graphql/graphql-client.js")) {
            if (is == null) {
                throw new IllegalStateException("graphql/graphql-client.js not found on classpath");
            }
            staticResourceProducer.produce(
                    new GeneratedStaticResourceBuildItem(
                            "/_static/quarkus-graphql/graphql-client.js",
                            IoUtil.readBytes(is)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (InputStream is = tccl.getResourceAsStream("graphql/graphql-client.d.ts")) {
            if (is == null) {
                throw new IllegalStateException("graphql/graphql-client.d.ts not found on classpath");
            }
            staticResourceProducer.produce(
                    new GeneratedStaticResourceBuildItem(
                            "/_static/quarkus-graphql/graphql-client.d.ts",
                            IoUtil.readBytes(is)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Schema schema = schemaBuildItem.getSchema();
        String resolvedPath = httpRootPathBuildItem.resolvePath(graphQLConfig.rootPath());
        String proxyJs = generateGraphQLProxy(schema, resolvedPath);
        staticResourceProducer.produce(
                new GeneratedStaticResourceBuildItem(
                        "/_static/quarkus-graphql-api/graphql-api.js",
                        proxyJs.getBytes(StandardCharsets.UTF_8)));

        String proxyDts = generateGraphQLDeclarations(schema);
        staticResourceProducer.produce(
                new GeneratedStaticResourceBuildItem(
                        "/_static/quarkus-graphql-api/graphql-api.d.ts",
                        proxyDts.getBytes(StandardCharsets.UTF_8)));
    }

    @BuildStep
    void registerGraphQLWebDependency(
            SmallRyeGraphQLConfig graphQLConfig,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<WebDependencyJarBuildItem> webDependencyProducer) {

        if (!graphQLConfig.jsClient().enabled()) {
            return;
        }

        curateOutcome.getApplicationModel().getDependencies().stream()
                .filter(dep -> dep.getGroupId().equals("io.quarkus")
                        && dep.getArtifactId().equals("quarkus-smallrye-graphql"))
                .findFirst()
                .ifPresent(dep -> webDependencyProducer.produce(new WebDependencyJarBuildItem(
                        dep.getKey(),
                        dep.getResolvedPaths().getSinglePath(),
                        Map.of(
                                "@quarkus/graphql", "/_static/quarkus-graphql/graphql-client.js",
                                "@quarkus/graphql/", "/_static/quarkus-graphql/",
                                "@quarkus/graphql-api", "/_static/quarkus-graphql-api/graphql-api.js"))));
    }

    private String generateGraphQLProxy(Schema schema, String endpointPath) {
        StringBuilder js = new StringBuilder();
        js.append("import { GraphQLClient } from '@quarkus/graphql';\n\n");
        js.append("export const client = new GraphQLClient({ endpoint: '")
                .append(escapeJs(endpointPath)).append("' });\n\n");

        generateOperationExports(js, "Queries", "query", schema.getQueries(), schema);
        generateOperationExports(js, "Mutations", "mutation", schema.getMutations(), schema);
        generateOperationExports(js, "Subscriptions", "subscription", schema.getSubscriptions(), schema);

        return js.toString();
    }

    private void generateOperationExports(StringBuilder js, String exportName, String operationKeyword,
            Set<Operation> operations, Schema schema) {
        if (operations == null || operations.isEmpty()) {
            js.append("export const ").append(exportName).append(" = {};\n\n");
            return;
        }

        js.append("export const ").append(exportName).append(" = {\n");
        List<Operation> sorted = operations.stream()
                .sorted(Comparator.comparing(Operation::getName))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            Operation op = sorted.get(i);
            String queryString = generateQueryString(op, operationKeyword, schema);
            String clientMethod = "subscription".equals(operationKeyword) ? "subscribe"
                    : "mutation".equals(operationKeyword) ? "mutate" : "query";
            js.append("    ").append(escapeJs(op.getName()))
                    .append(": (variables) => client.").append(clientMethod).append("('")
                    .append(escapeJs(queryString)).append("', variables)");
            if (i < sorted.size() - 1) {
                js.append(",");
            }
            js.append("\n");
        }
        js.append("};\n\n");
    }

    private String generateQueryString(Operation op, String operationKeyword, Schema schema) {
        StringBuilder qs = new StringBuilder();
        qs.append(operationKeyword).append(" ").append(op.getName());

        if (op.hasArguments()) {
            qs.append("(");
            List<Argument> args = op.getArguments();
            for (int i = 0; i < args.size(); i++) {
                Argument arg = args.get(i);
                qs.append("$").append(arg.getName()).append(": ").append(graphqlTypeName(arg));
                if (i < args.size() - 1) {
                    qs.append(", ");
                }
            }
            qs.append(")");
        }

        qs.append(" { ").append(op.getName());

        if (op.hasArguments()) {
            qs.append("(");
            List<Argument> args = op.getArguments();
            for (int i = 0; i < args.size(); i++) {
                Argument arg = args.get(i);
                qs.append(arg.getName()).append(": $").append(arg.getName());
                if (i < args.size() - 1) {
                    qs.append(", ");
                }
            }
            qs.append(")");
        }

        Reference returnRef = op.getReference();
        if (returnRef != null && needsSelectionSet(returnRef)) {
            String selectionSet = generateSelectionSet(returnRef, schema);
            if (!selectionSet.isEmpty()) {
                qs.append(" { ").append(selectionSet).append("}");
            }
        }

        qs.append(" }");
        return qs.toString();
    }

    private boolean needsSelectionSet(Reference ref) {
        ReferenceType type = ref.getType();
        return type == ReferenceType.TYPE || type == ReferenceType.INTERFACE;
    }

    private String generateSelectionSet(Reference ref, Schema schema) {
        Type type = schema.getTypes().get(ref.getName());
        if (type == null || type.getFields() == null || type.getFields().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Field> entry : new java.util.TreeMap<>(type.getFields()).entrySet()) {
            Field field = entry.getValue();
            Reference fieldRef = field.getReference();
            if (fieldRef != null) {
                ReferenceType fieldType = fieldRef.getType();
                if (fieldType == ReferenceType.SCALAR || fieldType == ReferenceType.ENUM) {
                    sb.append(field.getName()).append(" ");
                }
            }
        }
        return sb.toString();
    }

    private String graphqlTypeName(Field field) {
        Reference ref = field.getReference();
        String baseName = ref.getName();

        if (field.hasWrapper()) {
            Wrapper wrapper = field.getWrapper();
            if (wrapper.getWrapperType() == io.smallrye.graphql.schema.model.WrapperType.COLLECTION
                    || wrapper.getWrapperType() == io.smallrye.graphql.schema.model.WrapperType.ARRAY) {
                String inner = baseName;
                if (wrapper.isWrappedTypeNotNull()) {
                    inner += "!";
                }
                baseName = "[" + inner + "]";
            }
        }

        if (field.isNotNull()) {
            baseName += "!";
        }

        return baseName;
    }

    private static String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String generateGraphQLDeclarations(Schema schema) {
        StringBuilder ts = new StringBuilder();
        ts.append("import { GraphQLClient, Subscription } from '@quarkus/graphql';\n\n");
        ts.append("export declare const client: GraphQLClient;\n");

        if (schema.getEnums() != null) {
            for (Map.Entry<String, EnumType> entry : new java.util.TreeMap<>(schema.getEnums()).entrySet()) {
                EnumType enumType = entry.getValue();
                if (enumType.hasValues()) {
                    ts.append("\nexport type ").append(enumType.getName()).append(" = ");
                    List<EnumValue> values = new ArrayList<>(enumType.getValues());
                    for (int i = 0; i < values.size(); i++) {
                        ts.append("'").append(values.get(i).getValue()).append("'");
                        if (i < values.size() - 1) {
                            ts.append(" | ");
                        }
                    }
                    ts.append(";\n");
                }
            }
        }

        if (schema.getTypes() != null) {
            for (Map.Entry<String, Type> entry : new java.util.TreeMap<>(schema.getTypes()).entrySet()) {
                generateTypeInterface(ts, entry.getKey(), entry.getValue().getFields());
            }
        }

        if (schema.getInputs() != null) {
            for (Map.Entry<String, InputType> entry : new java.util.TreeMap<>(schema.getInputs()).entrySet()) {
                generateTypeInterface(ts, entry.getKey(), entry.getValue().getFields());
            }
        }

        generateOperationDeclarations(ts, "Queries", schema.getQueries(), schema, false);
        generateOperationDeclarations(ts, "Mutations", schema.getMutations(), schema, false);
        generateOperationDeclarations(ts, "Subscriptions", schema.getSubscriptions(), schema, true);

        return ts.toString();
    }

    private void generateTypeInterface(StringBuilder ts, String name, Map<String, Field> fields) {
        ts.append("\nexport interface ").append(name).append(" {\n");
        if (fields != null) {
            for (Map.Entry<String, Field> fieldEntry : new java.util.TreeMap<>(fields).entrySet()) {
                Field field = fieldEntry.getValue();
                String tsType = typescriptType(field);
                ts.append("    ").append(field.getName()).append(": ").append(tsType).append(";\n");
            }
        }
        ts.append("}\n");
    }

    private void generateOperationDeclarations(StringBuilder ts, String exportName, Set<Operation> operations,
            Schema schema, boolean isSubscription) {
        ts.append("\nexport declare const ").append(exportName).append(": {\n");
        if (operations != null && !operations.isEmpty()) {
            List<Operation> sorted = operations.stream()
                    .sorted(Comparator.comparing(Operation::getName))
                    .collect(Collectors.toList());

            for (int i = 0; i < sorted.size(); i++) {
                Operation op = sorted.get(i);
                ts.append("    ").append(op.getName()).append(": (");

                if (op.hasArguments()) {
                    ts.append("variables: { ");
                    List<Argument> args = op.getArguments();
                    for (int j = 0; j < args.size(); j++) {
                        Argument arg = args.get(j);
                        ts.append(arg.getName()).append(": ").append(typescriptType(arg));
                        if (j < args.size() - 1) {
                            ts.append("; ");
                        }
                    }
                    ts.append(" }");
                } else {
                    ts.append("variables?: Record<string, never>");
                }

                String returnType = typescriptReturnType(op, schema);
                if (isSubscription) {
                    ts.append(") => Subscription");
                } else {
                    ts.append(") => Promise<").append(returnType).append(">");
                }

                if (i < sorted.size() - 1) {
                    ts.append(";");
                }
                ts.append("\n");
            }
        }
        ts.append("};\n");
    }

    private String typescriptReturnType(Operation op, Schema schema) {
        Reference ref = op.getReference();
        if (ref == null) {
            return "unknown";
        }
        String baseType = typescriptBaseType(ref);
        if (op.hasWrapper()) {
            Wrapper wrapper = op.getWrapper();
            if (wrapper.getWrapperType() == io.smallrye.graphql.schema.model.WrapperType.COLLECTION
                    || wrapper.getWrapperType() == io.smallrye.graphql.schema.model.WrapperType.ARRAY) {
                return baseType + "[]";
            }
        }
        return baseType;
    }

    private String typescriptType(Field field) {
        Reference ref = field.getReference();
        if (ref == null) {
            return "unknown";
        }
        String baseType = typescriptBaseType(ref);

        if (field.hasWrapper()) {
            Wrapper wrapper = field.getWrapper();
            if (wrapper.getWrapperType() == io.smallrye.graphql.schema.model.WrapperType.COLLECTION
                    || wrapper.getWrapperType() == io.smallrye.graphql.schema.model.WrapperType.ARRAY) {
                baseType = baseType + "[]";
            }
        }

        if (!field.isNotNull()) {
            baseType += " | null";
        }

        return baseType;
    }

    private static final Map<String, String> GRAPHQL_TO_TS = Map.ofEntries(
            Map.entry("String", "string"),
            Map.entry("ID", "string"),
            Map.entry("Boolean", "boolean"),
            Map.entry("Int", "number"),
            Map.entry("Float", "number"),
            Map.entry("BigInteger", "number"),
            Map.entry("BigDecimal", "number"),
            Map.entry("Date", "string"),
            Map.entry("DateTime", "string"),
            Map.entry("Time", "string"),
            Map.entry("Period", "string"),
            Map.entry("Duration", "string"));

    private String typescriptBaseType(Reference ref) {
        String name = ref.getName();
        String mapped = GRAPHQL_TO_TS.get(name);
        if (mapped != null) {
            return mapped;
        }
        if (ref.getType() == ReferenceType.SCALAR) {
            return "unknown";
        }
        return name;
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void buildSchemaEndpoint(
            BuildProducer<RouteBuildItem> routeProducer,
            HttpRootPathBuildItem httpRootPathBuildItem,
            SmallRyeGraphQLInitializedBuildItem graphQLInitializedBuildItem,
            SmallRyeGraphQLRecorder recorder,
            SmallRyeGraphQLConfig graphQLConfig) {

        Handler<RoutingContext> schemaHandler = recorder.schemaHandler(graphQLInitializedBuildItem.getInitialized(),
                graphQLConfig.schemaAvailable());

        routeProducer.produce(httpRootPathBuildItem.routeBuilder()
                .nestedRoute(graphQLConfig.rootPath(), SCHEMA_PATH)
                .handler(schemaHandler)
                .displayOnNotFoundPage("MicroProfile GraphQL Schema")
                .build());

    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    void buildExecutionEndpoint(
            BuildProducer<RouteBuildItem> routeProducer,
            HttpRootPathBuildItem httpRootPathBuildItem,
            SmallRyeGraphQLInitializedBuildItem graphQLInitializedBuildItem,
            SmallRyeGraphQLRecorder recorder,
            ShutdownContextBuildItem shutdownContext,
            LaunchModeBuildItem launchMode,
            BodyHandlerBuildItem bodyHandlerBuildItem,
            SmallRyeGraphQLConfig graphQLConfig,
            BeanContainerBuildItem beanContainer,
            BuildProducer<WebsocketSubProtocolsBuildItem> webSocketSubProtocols,
            VertxHttpBuildTimeConfig httpBuildTimeConfig) {

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

        boolean runBlocking = shouldRunBlockingRoute(graphQLConfig);

        // Subscriptions
        Handler<RoutingContext> graphqlOverWebsocketHandler = recorder
                .graphqlOverWebsocketHandler(beanContainer.getValue(), graphQLInitializedBuildItem.getInitialized(),
                        runBlocking);

        HttpRootPathBuildItem.Builder subscriptionsBuilder = httpRootPathBuildItem.routeBuilder()
                .orderedRoute(graphQLConfig.rootPath(), GRAPHQL_WEBSOCKET_HANDLER_ORDER)
                .handler(graphqlOverWebsocketHandler);
        routeProducer.produce(subscriptionsBuilder.build());

        // WebSocket subprotocols
        graphQLConfig.websocketSubprotocols().ifPresentOrElse(subprotocols -> {
            for (String subprotocol : subprotocols) {
                if (!SUPPORTED_WEBSOCKET_SUBPROTOCOLS.contains(subprotocol)) {
                    throw new IllegalArgumentException("Unknown websocket subprotocol: " + subprotocol);
                } else {
                    webSocketSubProtocols.produce(new WebsocketSubProtocolsBuildItem(subprotocol));
                }
            }
        }, () -> {
            // if unspecified, allow all supported subprotocols
            for (String subprotocol : SUPPORTED_WEBSOCKET_SUBPROTOCOLS) {
                webSocketSubProtocols.produce(new WebsocketSubProtocolsBuildItem(subprotocol));
            }
        });

        // Queries and Mutations
        boolean allowCompression = httpBuildTimeConfig.enableCompression() && httpBuildTimeConfig.compressMediaTypes()
                .map(mediaTypes -> mediaTypes.contains(GRAPHQL_MEDIA_TYPE))
                .orElse(false);
        Handler<RoutingContext> executionHandler = recorder.executionHandler(graphQLInitializedBuildItem.getInitialized(),
                runBlocking, allowCompression);

        HttpRootPathBuildItem.Builder requestBuilder = httpRootPathBuildItem.routeBuilder()
                .routeFunction(graphQLConfig.rootPath(), recorder.routeFunction(bodyHandlerBuildItem.getHandler()))
                .handler(executionHandler)
                .routeConfigKey("quarkus.smallrye-graphql.root-path")
                .displayOnNotFoundPage("MicroProfile GraphQL Endpoint");

        if (runBlocking) {
            requestBuilder = requestBuilder.blockingRoute();
        }

        routeProducer.produce(requestBuilder.build());

    }

    private Set<String> getAllAdapterClasses(IndexView index) {
        Set<String> adapterClasses = new HashSet<>();
        adapterClasses.addAll(getAdapterClasses(index, DotName.createSimple(AdaptWith.class.getName())));
        adapterClasses.addAll(
                getAdapterClasses(index, DotName.createSimple("jakarta.json.bind.annotation.JsonbTypeAdapter")));
        adapterClasses.addAll(
                getAdapterClasses(index, DotName.createSimple("jakarta.json.bind.annotation.JsonbTypeAdapter")));
        return adapterClasses;
    }

    private Set<String> getAdapterClasses(IndexView index, DotName adapterClass) {
        Set<String> adapterClasses = new HashSet<>();
        Collection<AnnotationInstance> adaptWithAnnotations = index.getAnnotations(adapterClass);
        for (AnnotationInstance adaptWithAnnotation : adaptWithAnnotations) {
            AnnotationValue annotationValue = adaptWithAnnotation.value();
            if (annotationValue != null) {
                org.jboss.jandex.Type classType = annotationValue.asClass();
                adapterClasses.add(classType.name().toString());
            }
        }
        return adapterClasses;
    }

    private boolean shouldRunBlockingRoute(SmallRyeGraphQLConfig graphQLConfig) {
        if (graphQLConfig.nonBlockingEnabled().isPresent()) {
            return !graphQLConfig.nonBlockingEnabled().get();
        }
        return false;
    }

    private String[] getSchemaJavaClasses(Schema schema) {
        // Unique list of classes we need to do reflection on
        Set<String> classes = new HashSet<>();
        classes.addAll(getOperationClassNames(schema.getAllOperations()));
        classes.addAll(getTypeClassNames(schema.getTypes().values()));
        classes.addAll(getInputClassNames(schema.getInputs().values()));
        classes.addAll(getInterfaceClassNames(schema.getInterfaces().values()));
        classes.addAll(getUnionClassNames(schema.getUnions().values()));
        classes.addAll(getDirectiveTypeClassNames(schema.getDirectiveTypes()));

        return classes.toArray(String[]::new);
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
        classes.add(graphql.schema.GraphQLUnionType.class);
        classes.add(graphql.schema.GraphQLList.class);
        classes.add(graphql.schema.GraphQLNonNull.class);
        classes.add(graphql.schema.GraphQLObjectType.class);
        classes.add(graphql.schema.GraphQLOutputType.class);
        classes.add(graphql.schema.GraphQLScalarType.class);
        classes.add(graphql.schema.GraphQLSchema.class);
        classes.add(graphql.schema.GraphQLTypeReference.class);
        classes.add(List.class);
        classes.add(Collection.class);
        return classes.toArray(Class[]::new);
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

    private Set<String> getTypeClassNames(Collection<Type> complexGraphQLTypes) {
        Set<String> classes = new HashSet<>();
        for (Type complexGraphQLType : complexGraphQLTypes) {
            classes.add(complexGraphQLType.getClassName());
            classes.addAll(getFieldClassNames(complexGraphQLType.getFields()));
        }
        return classes;
    }

    private Set<String> getDirectiveTypeClassNames(Collection<DirectiveType> complexGraphQLDirectiveTypes) {
        Set<String> classes = new HashSet<>();
        for (DirectiveType complexGraphQLDirectiveType : complexGraphQLDirectiveTypes) {
            if (complexGraphQLDirectiveType.getClassName() != null) {
                classes.add(complexGraphQLDirectiveType.getClassName());
            }
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

    private Set<String> getInterfaceClassNames(Collection<Type> complexGraphQLTypes) {
        Set<String> classes = new HashSet<>();
        for (Type complexGraphQLType : complexGraphQLTypes) {
            classes.add(complexGraphQLType.getClassName());
            classes.addAll(getFieldClassNames(complexGraphQLType.getFields()));
        }
        return classes;
    }

    private Set<String> getUnionClassNames(Collection<UnionType> unionTypes) {
        Set<String> classes = new HashSet<>();
        for (UnionType unionType : unionTypes) {
            classes.add(unionType.getClassName());
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
        if (reference.getClassParametrizedTypes() != null && !reference.getClassParametrizedTypes().isEmpty()) {

            Collection<Reference> parametrized = reference.getClassParametrizedTypes().values();
            for (Reference r : parametrized) {
                classes.addAll(getAllReferenceClasses(r));
            }
        }
        return classes;
    }

    @BuildStep
    void printDataFetcherExceptionInDevMode(SmallRyeGraphQLConfig graphQLConfig,
            LaunchModeBuildItem launchMode,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {

        // User did not set this explicitly
        if (!graphQLConfig.printDataFetcherException().isPresent()) {
            if (launchMode.getLaunchMode().isDevOrTest()) {
                systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.PRINT_DATAFETCHER_EXCEPTION, TRUE));
            }
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.PRINT_DATAFETCHER_EXCEPTION,
                    String.valueOf(graphQLConfig.printDataFetcherException().get())));
        }
    }
    // Services Integrations

    @BuildStep
    void activateMetrics(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties, BuildProducer<ServiceProviderBuildItem> serviceProvider) {

        if (graphQLConfig.metricsEnabled().orElse(false)
                || Config.get().getConfigValue(ConfigKey.ENABLE_METRICS, boolean.class, false)) {
            metricsCapability.ifPresentOrElse(capability -> {
                if (capability.metricsSupported(MetricsFactory.MICROMETER)) {
                    serviceProvider.produce(new ServiceProviderBuildItem("io.smallrye.graphql.spi.MetricsService",
                            "io.smallrye.graphql.cdi.metrics.MicrometerMetricsService"));
                }
            }, () -> LOG
                    .warn("GraphQL metrics are enabled but no supported metrics implementation is available on the classpath"));
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_METRICS, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_METRICS, FALSE));
        }
    }

    @BuildStep
    void activateTracing(Capabilities capabilities,
            SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        boolean activate = shouldActivateService(graphQLConfig.tracingEnabled(),
                capabilities.isPresent(Capability.OPENTELEMETRY_TRACER),
                "quarkus-opentelemetry",
                Capability.OPENTELEMETRY_TRACER,
                "quarkus.smallrye-graphql.tracing.enabled",
                true);
        if (activate) {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_TRACING, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_TRACING, FALSE));
        }
    }

    @BuildStep
    void activateEventing(SmallRyeGraphQLConfig graphQLConfig, BuildProducer<SystemPropertyBuildItem> systemProperties) {
        if (graphQLConfig.eventsEnabled()) {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_EVENTS, TRUE));
        } else {
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_EVENTS, FALSE));
        }
    }

    @BuildStep
    void activateFederationBatchResolving(SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {
        if (graphQLConfig.federationBatchResolvingEnabled().isPresent()) {
            String value = graphQLConfig.federationBatchResolvingEnabled().get().toString();
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_FEDERATION_BATCH_RESOLVING, value));
            System.setProperty(ConfigKey.ENABLE_FEDERATION_BATCH_RESOLVING, value);
        }
    }

    /*
     * Decides whether we want to activate GraphQL federation and updates system properties accordingly.
     * If quarkus.smallrye-graphql.federation.enabled is unspecified, enable federation automatically if we see
     * any Federation annotations in the app.
     * If it is specified, always respect that setting.
     *
     * This would normally be a separate step like other similar activations, but it updates
     * system properties and needs to run before generating the schema, so it is called
     * by the build step that generates the schema.
     *
     * Apart from generating a SystemPropertyBuildItem, it's necessary to also call
     * System.setProperty to make sure that the SchemaBuilder, called at build time, sees the correct value.
     */
    void activateFederation(SmallRyeGraphQLConfig config,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            SmallRyeGraphQLFinalIndexBuildItem index) {
        if (config.federationEnabled().isPresent()) {
            String value = config.federationEnabled().get().toString();
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_FEDERATION, value));
            System.setProperty(ConfigKey.ENABLE_FEDERATION, value);
        } else {
            //
            boolean foundAnyFederationAnnotation = false;
            for (ClassInfo federationAnnotationType : index.getFinalIndex()
                    .getClassesInPackage("io.smallrye.graphql.api.federation")) {
                if (federationAnnotationType.isAnnotation()) {
                    if (!index.getFinalIndex().getAnnotations(federationAnnotationType.name()).isEmpty()) {
                        foundAnyFederationAnnotation = true;
                    }
                }
            }
            String value = Boolean.toString(foundAnyFederationAnnotation);
            systemProperties.produce(new SystemPropertyBuildItem(ConfigKey.ENABLE_FEDERATION, value));
            System.setProperty(ConfigKey.ENABLE_FEDERATION, value);
        }
    }

    private boolean shouldActivateService(Optional<Boolean> serviceEnabled,
            boolean linkedCapabilityIsPresent,
            String linkedExtensionName,
            String linkedCapabilityName,
            String configKey,
            boolean activateByDefaultIfCapabilityIsPresent) {

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
            return linkedCapabilityIsPresent && activateByDefaultIfCapabilityIsPresent;
        }
    }

    // UI Related

    @BuildStep
    void getGraphqlUiFinalDestination(
            HttpRootPathBuildItem httpRootPath,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeGraphQLConfig graphQLConfig,
            BuildProducer<WebJarBuildItem> webJarBuildProducer) {

        if (shouldInclude(launchMode, graphQLConfig)) {

            if ("/".equals(graphQLConfig.ui().rootPath())) {
                throw new ConfigurationException(
                        "quarkus.smallrye-graphql.root-path-ui was set to \"/\", this is not allowed as it blocks the application from serving anything else.",
                        Collections.singleton("quarkus.smallrye-graphql.root-path-ui"));
            }

            String graphQLPath = httpRootPath.resolvePath(graphQLConfig.rootPath());
            String graphQLUiPath = nonApplicationRootPathBuildItem.resolvePath(graphQLConfig.ui().rootPath());
            String devUiPath = nonApplicationRootPathBuildItem.resolvePath("dev");

            webJarBuildProducer.produce(
                    WebJarBuildItem.builder().artifactKey(GRAPHQL_UI_WEBJAR_ARTIFACT_KEY) //
                            .root(GRAPHQL_UI_WEBJAR_STATIC_RESOURCES_PATH) //
                            .filter(new WebJarResourcesFilter() {
                                @Override
                                public FilterResult apply(String fileName, InputStream file) throws IOException {
                                    if (fileName.endsWith(FILE_TO_UPDATE)) {
                                        String content = new String(file.readAllBytes(), StandardCharsets.UTF_8);
                                        content = updateUrl(content, graphQLPath, LINE_TO_UPDATE,
                                                LINE_FORMAT);
                                        content = updateUrl(content, graphQLUiPath,
                                                UI_LINE_TO_UPDATE,
                                                UI_LINE_FORMAT);
                                        content = updateUrl(content, getLogoUrl(launchMode, devUiPath, graphQLUiPath),
                                                LOGO_LINE_TO_UPDATE,
                                                LOGO_LINE_FORMAT);

                                        return new FilterResult(
                                                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true);
                                    }

                                    return new FilterResult(file, false);
                                }
                            })
                            .build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerGraphQLUiHandler(
            BuildProducer<RouteBuildItem> routeProducer,
            SmallRyeGraphQLRecorder recorder,
            LaunchModeBuildItem launchMode,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeGraphQLConfig graphQLConfig,
            WebJarResultsBuildItem webJarResultsBuildItem,
            BuildProducer<SmallRyeGraphQLBuildItem> smallRyeGraphQLBuildProducer, ShutdownContextBuildItem shutdownContext) {

        WebJarResultsBuildItem.WebJarResult result = webJarResultsBuildItem.byArtifactKey(GRAPHQL_UI_WEBJAR_ARTIFACT_KEY);
        if (result == null) {
            return;
        }

        if (shouldInclude(launchMode, graphQLConfig)) {
            String graphQLUiPath = nonApplicationRootPathBuildItem.resolvePath(graphQLConfig.ui().rootPath());
            smallRyeGraphQLBuildProducer
                    .produce(new SmallRyeGraphQLBuildItem(result.getFinalDestination(), graphQLUiPath));

            Handler<RoutingContext> handler = recorder.uiHandler(result.getFinalDestination(),
                    graphQLUiPath, result.getWebRootConfigurations(), shutdownContext);
            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(graphQLConfig.ui().rootPath())
                    .displayOnNotFoundPage("GraphQL UI")
                    .routeConfigKey("quarkus.smallrye-graphql.ui.root-path")
                    .handler(handler)
                    .build());

            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(graphQLConfig.ui().rootPath() + "*")
                    .handler(handler)
                    .build());

        }
    }

    @BuildStep
    void indexPanacheClasses(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses) {
        // so that they can be used in SmallRye GraphQL queries
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.panache.common.Sort$Direction")) {
            additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem("io.quarkus.panache.common.Sort$Direction"));
        }
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.panache.common.Sort$NullPrecedence")) {
            additionalIndexedClasses
                    .produce(new AdditionalIndexedClassesBuildItem("io.quarkus.panache.common.Sort$NullPrecedence"));
        }
    }

    // This build step can be removed after Quarkus updated to any version newer than 2.14.1
    // See also https://github.com/smallrye/smallrye-graphql/pull/2299
    @BuildStep
    void registerKnownSpecializationAnnotation(
            BuildProducer<KnownCompatibleBeanArchiveBuildItem> compatArchiveProducer) {
        compatArchiveProducer.produce(KnownCompatibleBeanArchiveBuildItem.builder("io.smallrye", "smallrye-graphql-cdi")
                .addReason(KnownCompatibleBeanArchiveBuildItem.Reason.SPECIALIZES_ANNOTATION).build());
    }

    // In dev mode, when you click on the logo, you should go to Dev UI
    private String getLogoUrl(LaunchModeBuildItem launchMode, String devUIValue, String defaultValue) {
        if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)) {
            return devUIValue;
        }
        return defaultValue;
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, SmallRyeGraphQLConfig graphQLConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || graphQLConfig.ui().alwaysInclude();
    }

    private String updateUrl(String original, String path, String lineStartsWith, String format) {
        try (Scanner scanner = new Scanner(original)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().startsWith(lineStartsWith)) {
                    String newLine = String.format(format, path);
                    return original.replace(line.trim(), newLine);
                }
            }
        }

        return original;
    }
}
