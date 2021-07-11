package io.quarkus.smallrye.openapi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.server.common.spi.AllowedJaxRsAnnotationPrefixBuildItem;
import io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.deployment.security.SecurityConfigFilter;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService;
import io.quarkus.smallrye.openapi.runtime.OpenApiRecorder;
import io.quarkus.smallrye.openapi.runtime.OpenApiRuntimeConfig;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.AnnotationScannerExtension;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import io.smallrye.openapi.vertx.VertxConstants;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The main OpenAPI Processor. This will scan for JAX-RS, Spring and Vert.x Annotations, and, if any, add supplied schemas.
 * The result is added to the deployable unit to be loaded at runtime.
 */
public class SmallRyeOpenApiProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.smallrye.openapi");

    private static final String META_INF_OPENAPI_YAML = "META-INF/openapi.yaml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YAML = "WEB-INF/classes/META-INF/openapi.yaml";
    private static final String META_INF_OPENAPI_YML = "META-INF/openapi.yml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YML = "WEB-INF/classes/META-INF/openapi.yml";
    private static final String META_INF_OPENAPI_JSON = "META-INF/openapi.json";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_JSON = "WEB-INF/classes/META-INF/openapi.json";

    private static final DotName OPENAPI_SCHEMA = DotName.createSimple(Schema.class.getName());
    private static final DotName OPENAPI_RESPONSE = DotName.createSimple(APIResponse.class.getName());
    private static final DotName OPENAPI_RESPONSES = DotName.createSimple(APIResponses.class.getName());

    private static final String OPENAPI_RESPONSE_CONTENT = "content";
    private static final String OPENAPI_RESPONSE_SCHEMA = "schema";
    private static final String OPENAPI_SCHEMA_NOT = "not";
    private static final String OPENAPI_SCHEMA_ONE_OF = "oneOf";
    private static final String OPENAPI_SCHEMA_ANY_OF = "anyOf";
    private static final String OPENAPI_SCHEMA_ALL_OF = "allOf";
    private static final String OPENAPI_SCHEMA_IMPLEMENTATION = "implementation";
    private static final String JAX_RS = "JAX-RS";
    private static final String SPRING = "Spring";
    private static final String VERT_X = "Vert.x";

    static {
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_PRODUCES, "application/json");
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_CONSUMES, "application/json");
    }

    @BuildStep
    void mapConfig(SmallRyeOpenApiConfig openApiConfig,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {

        if (openApiConfig.openApiVersion.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.OPEN_API_VERSION, openApiConfig.openApiVersion.get()));
        }
        if (openApiConfig.infoTitle.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(io.smallrye.openapi.api.constants.OpenApiConstants.INFO_TITLE,
                    openApiConfig.infoTitle.get()));
        }
        if (openApiConfig.infoVersion.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.INFO_VERSION, openApiConfig.infoVersion.get()));
        }
        if (openApiConfig.infoDescription.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.INFO_DESCRIPTION, openApiConfig.infoDescription.get()));
        }
        if (openApiConfig.infoTermsOfService.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(io.smallrye.openapi.api.constants.OpenApiConstants.INFO_TERMS,
                    openApiConfig.infoTermsOfService.get()));
        }
        if (openApiConfig.infoContactEmail.isPresent()) {
            systemProperties
                    .produce(new SystemPropertyBuildItem(io.smallrye.openapi.api.constants.OpenApiConstants.INFO_CONTACT_EMAIL,
                            openApiConfig.infoContactEmail.get()));
        }
        if (openApiConfig.infoContactName.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.INFO_CONTACT_NAME, openApiConfig.infoContactName.get()));
        }
        if (openApiConfig.infoContactUrl.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.INFO_CONTACT_URL, openApiConfig.infoContactUrl.get()));
        }
        if (openApiConfig.infoLicenseName.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.INFO_LICENSE_NAME, openApiConfig.infoLicenseName.get()));
        }
        if (openApiConfig.infoLicenseUrl.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(
                    io.smallrye.openapi.api.constants.OpenApiConstants.INFO_LICENSE_URL, openApiConfig.infoLicenseUrl.get()));
        }
        if (openApiConfig.operationIdStrategy.isPresent()) {
            systemProperties.produce(
                    new SystemPropertyBuildItem(io.smallrye.openapi.api.constants.OpenApiConstants.OPERATION_ID_STRAGEGY,
                            openApiConfig.operationIdStrategy.get().name()));
        }
    }

    @BuildStep
    void contributeClassesToIndex(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses) {
        // contribute additional JDK classes to the index, because SmallRye OpenAPI will check if some
        // app types implement Map and Collection and will go through super classes until Object is reached,
        // and yes, it even checks Object
        // see https://github.com/quarkusio/quarkus/issues/2961
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(
                Collection.class.getName(),
                Map.class.getName(),
                Object.class.getName()));
    }

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> configFiles() {
        return Stream.of(META_INF_OPENAPI_YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML,
                META_INF_OPENAPI_YML, WEB_INF_CLASSES_META_INF_OPENAPI_YML,
                META_INF_OPENAPI_JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON).map(HotDeploymentWatchedFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    RouteBuildItem handler(LaunchModeBuildItem launch,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            OpenApiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            OpenApiRuntimeConfig openApiRuntimeConfig,
            ShutdownContextBuildItem shutdownContext,
            SmallRyeOpenApiConfig openApiConfig) {
        /*
         * <em>Ugly Hack</em>
         * In dev mode, we pass a classloader to load the up to date OpenAPI document.
         * This hack is required because using the TCCL would get an outdated version - the initial one.
         * This is because the worker thread on which the handler is called captures the TCCL at creation time
         * and does not allow updating it.
         *
         * This classloader must ONLY be used to load the OpenAPI document.
         *
         * In non dev mode, the TCCL is used.
         */
        if (launch.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            recorder.setupClDevMode(shutdownContext);
        }

        Handler<RoutingContext> handler = recorder.handler(openApiRuntimeConfig);
        return nonApplicationRootPathBuildItem.routeBuilder()
                .route(openApiConfig.path)
                .routeConfigKey("quarkus.smallrye-openapi.path")
                .handler(handler)
                .displayOnNotFoundPage("Open API Schema document")
                .blockingRoute()
                .build();
    }

    @BuildStep
    void addSecurityFilter(BuildProducer<AddToOpenAPIDefinitionBuildItem> addToOpenAPIDefinitionProducer,
            SmallRyeOpenApiConfig config) {

        addToOpenAPIDefinitionProducer
                .produce(new AddToOpenAPIDefinitionBuildItem(new SecurityConfigFilter(config)));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void classLoaderHack(OpenApiRecorder recorder) {
        recorder.classLoaderHack();
    }

    @BuildStep
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(OpenApiDocumentService.class)
                .setUnremovable().build());
    }

    @BuildStep
    OpenApiFilteredIndexViewBuildItem smallryeOpenApiIndex(CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        CompositeIndex compositeIndex = CompositeIndex.create(combinedIndexBuildItem.getIndex(),
                beanArchiveIndexBuildItem.getIndex());
        return new OpenApiFilteredIndexViewBuildItem(
                new FilteredIndexView(
                        compositeIndex,
                        new OpenApiConfigImpl(ConfigProvider.getConfig())));
    }

    @BuildStep
    public List<AllowedJaxRsAnnotationPrefixBuildItem> registerJaxRsSupportedAnnotation() {
        List<AllowedJaxRsAnnotationPrefixBuildItem> prefixes = new ArrayList<>();
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("org.eclipse.microprofile.openapi.annotations"));
        return prefixes;
    }

    @BuildStep
    public void registerOpenApiSchemaClassesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem,
            Capabilities capabilities) {

        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();

        if (shouldScanAnnotations(capabilities, index)) {
            // Generate reflection declaration from MP OpenAPI Schema definition
            // They are needed for serialization.
            Collection<AnnotationInstance> schemaAnnotationInstances = index.getAnnotations(OPENAPI_SCHEMA);
            for (AnnotationInstance schemaAnnotationInstance : schemaAnnotationInstances) {
                AnnotationTarget typeTarget = schemaAnnotationInstance.target();
                if (typeTarget.kind() != AnnotationTarget.Kind.CLASS) {
                    continue;
                }
                produceReflectiveHierarchy(reflectiveHierarchy, Type.create(typeTarget.asClass().name(), Type.Kind.CLASS),
                        getClass().getSimpleName() + " > " + typeTarget.asClass().name());
            }

            // Generate reflection declaration from MP OpenAPI APIResponse schema definition
            // They are needed for serialization
            Collection<AnnotationInstance> apiResponseAnnotationInstances = index.getAnnotations(OPENAPI_RESPONSE);
            registerReflectionForApiResponseSchemaSerialization(reflectiveClass, reflectiveHierarchy,
                    apiResponseAnnotationInstances);

            // Generate reflection declaration from MP OpenAPI APIResponses schema definition
            // They are needed for serialization
            Collection<AnnotationInstance> apiResponsesAnnotationInstances = index.getAnnotations(OPENAPI_RESPONSES);
            for (AnnotationInstance apiResponsesAnnotationInstance : apiResponsesAnnotationInstances) {
                AnnotationValue apiResponsesAnnotationValue = apiResponsesAnnotationInstance.value();
                if (apiResponsesAnnotationValue == null) {
                    continue;
                }
                registerReflectionForApiResponseSchemaSerialization(reflectiveClass, reflectiveHierarchy,
                        Arrays.asList(apiResponsesAnnotationValue.asNestedArray()));
            }
        }
    }

    private void registerReflectionForApiResponseSchemaSerialization(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            Collection<AnnotationInstance> apiResponseAnnotationInstances) {
        for (AnnotationInstance apiResponseAnnotationInstance : apiResponseAnnotationInstances) {
            AnnotationValue contentAnnotationValue = apiResponseAnnotationInstance.value(OPENAPI_RESPONSE_CONTENT);
            if (contentAnnotationValue == null) {
                continue;
            }

            AnnotationInstance[] contents = contentAnnotationValue.asNestedArray();
            for (AnnotationInstance content : contents) {
                AnnotationValue annotationValue = content.value(OPENAPI_RESPONSE_SCHEMA);
                if (annotationValue == null) {
                    continue;
                }

                AnnotationInstance schema = annotationValue.asNested();
                String source = getClass().getSimpleName() + " > " + schema.target();

                AnnotationValue schemaImplementationClass = schema.value(OPENAPI_SCHEMA_IMPLEMENTATION);
                if (schemaImplementationClass != null) {
                    produceReflectiveHierarchy(reflectiveHierarchy, schemaImplementationClass.asClass(), source);
                }

                AnnotationValue schemaNotClass = schema.value(OPENAPI_SCHEMA_NOT);
                if (schemaNotClass != null) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, schemaNotClass.asString()));
                }

                produceReflectiveHierarchy(reflectiveHierarchy, schema.value(OPENAPI_SCHEMA_ONE_OF), source);
                produceReflectiveHierarchy(reflectiveHierarchy, schema.value(OPENAPI_SCHEMA_ANY_OF), source);
                produceReflectiveHierarchy(reflectiveHierarchy, schema.value(OPENAPI_SCHEMA_ALL_OF), source);
            }
        }
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem,
            Capabilities capabilities,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems,
            HttpRootPathBuildItem httpRootPathBuildItem,
            OutputTargetBuildItem out,
            SmallRyeOpenApiConfig openApiConfig,
            Optional<ResteasyJaxrsConfigBuildItem> resteasyJaxrsConfig) throws Exception {
        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_OPENAPI));
        OpenAPI staticModel = generateStaticModel(openApiConfig);

        OpenAPI annotationModel;

        if (shouldScanAnnotations(capabilities, index)) {
            annotationModel = generateAnnotationModel(index, capabilities, httpRootPathBuildItem, resteasyJaxrsConfig);
        } else {
            annotationModel = new OpenAPIImpl();
        }
        OpenApiDocument finalDocument = loadDocument(staticModel, annotationModel, openAPIBuildItems);

        for (Format format : Format.values()) {
            String name = OpenApiConstants.BASE_NAME + format;
            byte[] schemaDocument = OpenApiSerializer.serialize(finalDocument.get(), format).getBytes(StandardCharsets.UTF_8);
            resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(name, schemaDocument));
            nativeImageResources.produce(new NativeImageResourceBuildItem(name));
        }

        // Store the document if needed
        boolean shouldStore = openApiConfig.storeSchemaDirectory.isPresent();
        if (shouldStore) {
            storeDocument(out, openApiConfig, staticModel, annotationModel, openAPIBuildItems);
        }
    }

    @BuildStep
    LogCleanupFilterBuildItem logCleanup() {
        return new LogCleanupFilterBuildItem("io.smallrye.openapi.api.OpenApiDocument",
                "OpenAPI document initialized:");
    }

    private void produceReflectiveHierarchy(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            AnnotationValue annotationValue, String source) {
        if (annotationValue != null) {
            for (Type type : annotationValue.asClassArray()) {
                produceReflectiveHierarchy(reflectiveHierarchy, type, source);
            }
        }
    }

    private void produceReflectiveHierarchy(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, Type type,
            String source) {
        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(type)
                .ignoreTypePredicate(ResteasyDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                .ignoreFieldPredicate(ResteasyDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                .ignoreMethodPredicate(ResteasyDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                .source(source)
                .build());
    }

    private void storeGeneratedSchema(SmallRyeOpenApiConfig openApiConfig, OutputTargetBuildItem out, byte[] schemaDocument,
            Format format) throws IOException {
        Path directory = openApiConfig.storeSchemaDirectory.get();

        Path outputDirectory = out.getOutputDirectory();

        if (!directory.isAbsolute() && outputDirectory != null) {
            directory = Paths.get(outputDirectory.getParent().toString(), directory.toString());
        }

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path file = Paths.get(directory.toString(), "openapi." + format.toString().toLowerCase());
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        Files.write(file, schemaDocument, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("OpenAPI " + format.toString() + " saved: " + file.toString());
    }

    private boolean shouldScanAnnotations(Capabilities capabilities, IndexView index) {
        // Disabled via config
        Config config = ConfigProvider.getConfig();
        boolean scanDisable = config.getOptionalValue(OASConfig.SCAN_DISABLE, Boolean.class).orElse(false);
        if (scanDisable) {
            return false;
        }

        // Only scan if either RESTEasy, Quarkus REST, Spring Web or Vert.x Web (with @Route) is used
        boolean isRestEasy = capabilities.isPresent(Capability.RESTEASY);
        boolean isQuarkusRest = capabilities.isPresent(Capability.RESTEASY_REACTIVE);
        boolean isSpring = capabilities.isPresent(Capability.SPRING_WEB);
        boolean isVertx = isUsingVertxRoute(index);
        return isRestEasy || isQuarkusRest || isSpring || isVertx;
    }

    private boolean isUsingVertxRoute(IndexView index) {
        if (!index.getAnnotations(VertxConstants.ROUTE).isEmpty()
                || !index.getAnnotations(VertxConstants.ROUTE_BASE).isEmpty()) {
            return true;
        }
        return false;
    }

    private OpenAPI generateStaticModel(SmallRyeOpenApiConfig openApiConfig) throws IOException {
        if (openApiConfig.ignoreStaticDocument) {
            return null;
        } else {
            Result result = findStaticModel();
            if (result != null) {
                try (InputStream is = result.inputStream;
                        OpenApiStaticFile staticFile = new OpenApiStaticFile(is, result.format)) {
                    return io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile);
                }
            }
            return null;
        }
    }

    private OpenAPI generateAnnotationModel(IndexView indexView, Capabilities capabilities,
            HttpRootPathBuildItem httpRootPathBuildItem,
            Optional<ResteasyJaxrsConfigBuildItem> resteasyJaxrsConfig) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        List<AnnotationScannerExtension> extensions = new ArrayList<>();
        // Add the RESTEasy extension if the capability is present
        if (capabilities.isPresent(Capability.RESTEASY)) {
            extensions.add(new RESTEasyExtension(indexView));
        }
        // TODO: add a Quarkus-REST specific extension that knows the Quarkus REST specific annotations as well as the fact that *param annotations aren't necessary

        String defaultPath;
        if (resteasyJaxrsConfig.isPresent()) {
            defaultPath = resteasyJaxrsConfig.get().getRootPath();
        } else {
            defaultPath = httpRootPathBuildItem.getRootPath();
        }
        if (defaultPath != null && !"/".equals(defaultPath)) {
            extensions.add(new CustomPathExtension(defaultPath));
        }

        OpenApiAnnotationScanner openApiAnnotationScanner = new OpenApiAnnotationScanner(openApiConfig, indexView, extensions);
        return openApiAnnotationScanner.scan(getScanners(capabilities, indexView));
    }

    private String[] getScanners(Capabilities capabilities, IndexView index) {
        List<String> scanners = new ArrayList<>();
        if (capabilities.isPresent(Capability.RESTEASY) || capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            scanners.add(JAX_RS);
        }
        if (capabilities.isPresent(Capability.SPRING_WEB)) {
            scanners.add(SPRING);
        }
        if (isUsingVertxRoute(index)) {
            scanners.add(VERT_X);
        }
        return scanners.toArray(new String[] {});
    }

    private Result findStaticModel() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // Check for the file in both META-INF and WEB-INF/classes/META-INF
        Format format = Format.YAML;
        InputStream inputStream = cl.getResourceAsStream(META_INF_OPENAPI_YAML);
        if (inputStream == null) {
            inputStream = cl.getResourceAsStream(WEB_INF_CLASSES_META_INF_OPENAPI_YAML);
        }
        if (inputStream == null) {
            inputStream = cl.getResourceAsStream(META_INF_OPENAPI_YML);
        }
        if (inputStream == null) {
            inputStream = cl.getResourceAsStream(WEB_INF_CLASSES_META_INF_OPENAPI_YML);
        }
        if (inputStream == null) {
            inputStream = cl.getResourceAsStream(META_INF_OPENAPI_JSON);
            format = Format.JSON;
        }
        if (inputStream == null) {
            inputStream = cl.getResourceAsStream(WEB_INF_CLASSES_META_INF_OPENAPI_JSON);
            format = Format.JSON;
        }

        if (inputStream == null) {
            return null;
        }

        return new Result(format, inputStream);
    }

    static class Result {
        final Format format;
        final InputStream inputStream;

        Result(Format format, InputStream inputStream) {
            this.format = format;
            this.inputStream = inputStream;
        }
    }

    private OpenApiDocument loadDocument(OpenAPI staticModel, OpenAPI annotationModel,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems) {
        OpenApiDocument document = prepareOpenApiDocument(staticModel, annotationModel, openAPIBuildItems);
        document.initialize();
        return document;
    }

    private void storeDocument(OutputTargetBuildItem out,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            OpenAPI staticModel,
            OpenAPI annotationModel,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems) throws IOException {

        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        OpenApiDocument document = prepareOpenApiDocument(staticModel, annotationModel, openAPIBuildItems);

        document.filter(filter(openApiConfig)); // This usually happens at runtime, so when storing we want to filter here too.
        document.initialize();

        for (Format format : Format.values()) {
            String name = OpenApiConstants.BASE_NAME + format;
            byte[] schemaDocument = OpenApiSerializer.serialize(document.get(), format).getBytes(StandardCharsets.UTF_8);
            storeGeneratedSchema(smallRyeOpenApiConfig, out, schemaDocument, format);
        }

    }

    private OpenApiDocument prepareOpenApiDocument(OpenAPI staticModel,
            OpenAPI annotationModel,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig,
                Thread.currentThread().getContextClassLoader());

        OpenApiDocument document = createDocument(openApiConfig);
        if (annotationModel != null) {
            document.modelFromAnnotations(annotationModel);
        }
        document.modelFromReader(readerModel);
        document.modelFromStaticFile(staticModel);
        for (AddToOpenAPIDefinitionBuildItem openAPIBuildItem : openAPIBuildItems) {
            OASFilter otherExtensionFilter = openAPIBuildItem.getOASFilter();
            document.filter(otherExtensionFilter);
        }
        return document;
    }

    private OpenApiDocument createDocument(OpenApiConfig openApiConfig) {
        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);
        return document;
    }

    private OASFilter filter(OpenApiConfig openApiConfig) {
        return OpenApiProcessor.getFilter(openApiConfig,
                Thread.currentThread().getContextClassLoader());
    }
}
