package io.quarkus.smallrye.openapi.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
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
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.server.common.spi.AllowedJaxRsAnnotationPrefixBuildItem;
import io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.deployment.filter.AutoRolesAllowedFilter;
import io.quarkus.smallrye.openapi.deployment.filter.AutoTagFilter;
import io.quarkus.smallrye.openapi.deployment.filter.SecurityConfigFilter;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService;
import io.quarkus.smallrye.openapi.runtime.OpenApiRecorder;
import io.quarkus.smallrye.openapi.runtime.OpenApiRuntimeConfig;
import io.quarkus.smallrye.openapi.runtime.filter.AutoBasicSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoJWTSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoUrl;
import io.quarkus.smallrye.openapi.runtime.filter.OpenIDConnectSecurityFilter;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.constants.SecurityConstants;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.util.MergeUtil;
import io.smallrye.openapi.jaxrs.JaxRsConstants;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.AnnotationScannerExtension;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import io.smallrye.openapi.runtime.util.JandexUtil;
import io.smallrye.openapi.spring.SpringConstants;
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
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> serviceProvider) throws IOException {
        // To map from smallrye and mp config to quarkus
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(OpenApiConfigMapping.class.getName()));
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
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiRuntimeConfig openApiRuntimeConfig,
            ShutdownContextBuildItem shutdownContext,
            SmallRyeOpenApiConfig openApiConfig,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            HttpConfiguration httpConfiguration) {
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

        OASFilter autoSecurityFilter = null;
        if (openApiConfig.autoAddSecurity) {
            // Only add the security if there are secured endpoints
            OASFilter autoRolesAllowedFilter = getAutoRolesAllowedFilter(openApiConfig.securitySchemeName,
                    apiFilteredIndexViewBuildItem, openApiConfig);
            if (autoRolesAllowedFilter != null) {
                autoSecurityFilter = getAutoSecurityFilter(securityInformationBuildItems, openApiConfig);
            }
        }

        Handler<RoutingContext> handler = recorder.handler(openApiRuntimeConfig, httpConfiguration, autoSecurityFilter);
        return nonApplicationRootPathBuildItem.routeBuilder()
                .route(openApiConfig.path)
                .routeConfigKey("quarkus.smallrye-openapi.path")
                .handler(handler)
                .displayOnNotFoundPage("Open API Schema document")
                .blockingRoute()
                .build();
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
    void addSecurityFilter(BuildProducer<AddToOpenAPIDefinitionBuildItem> addToOpenAPIDefinitionProducer,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            SmallRyeOpenApiConfig config) {

        List<AnnotationInstance> rolesAllowedAnnotations = new ArrayList<>();
        for (DotName rolesAllowed : SecurityConstants.ROLES_ALLOWED) {
            rolesAllowedAnnotations.addAll(apiFilteredIndexViewBuildItem.getIndex().getAnnotations(rolesAllowed));
        }

        Map<String, List<String>> methodReferences = new HashMap<>();
        DotName securityRequirement = DotName.createSimple(SecurityRequirement.class.getName());
        for (AnnotationInstance ai : rolesAllowedAnnotations) {
            if (ai.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                MethodInfo method = ai.target().asMethod();
                if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                    String ref = JandexUtil.createUniqueMethodReference(method);
                    methodReferences.put(ref, List.of(ai.value().asStringArray()));
                }
            }
            if (ai.target().kind().equals(AnnotationTarget.Kind.CLASS)) {
                ClassInfo classInfo = ai.target().asClass();
                List<MethodInfo> methods = classInfo.methods();
                for (MethodInfo method : methods) {
                    if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                        String ref = JandexUtil.createUniqueMethodReference(method);
                        methodReferences.put(ref, List.of(ai.value().asStringArray()));
                    }
                }

            }
        }

        // Add a security scheme from config
        if (config.securityScheme.isPresent()) {
            addToOpenAPIDefinitionProducer
                    .produce(new AddToOpenAPIDefinitionBuildItem(
                            new SecurityConfigFilter(config)));
        }

        // Add Auto roles allowed
        OASFilter autoRolesAllowedFilter = getAutoRolesAllowedFilter(config.securitySchemeName, apiFilteredIndexViewBuildItem,
                config);
        if (autoRolesAllowedFilter != null) {
            addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(autoRolesAllowedFilter));
        }

        // Add Auto Tag based on the class name
        OASFilter autoTagFilter = getAutoTagFilter(apiFilteredIndexViewBuildItem,
                config);
        if (autoTagFilter != null) {
            addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(autoTagFilter));
        }

    }

    private OASFilter getAutoSecurityFilter(List<SecurityInformationBuildItem> securityInformationBuildItems,
            SmallRyeOpenApiConfig config) {

        // Auto add a security from security extension(s)
        if (!config.securityScheme.isPresent() && securityInformationBuildItems != null
                && !securityInformationBuildItems.isEmpty()) {
            // This needs to be a filter in runtime as the config we use to auto configure is in runtime
            for (SecurityInformationBuildItem securityInformationBuildItem : securityInformationBuildItems) {
                SecurityInformationBuildItem.SecurityModel securityModel = securityInformationBuildItem.getSecurityModel();
                switch (securityModel) {
                    case jwt:
                        return new AutoJWTSecurityFilter(
                                config.securitySchemeName,
                                config.securitySchemeDescription,
                                config.jwtSecuritySchemeValue,
                                config.jwtBearerFormat);
                    case basic:
                        return new AutoBasicSecurityFilter(
                                config.securitySchemeName,
                                config.securitySchemeDescription,
                                config.basicSecuritySchemeValue);
                    case oidc:
                        Optional<SecurityInformationBuildItem.OpenIDConnectInformation> maybeInfo = securityInformationBuildItem
                                .getOpenIDConnectInformation();

                        if (maybeInfo.isPresent()) {
                            SecurityInformationBuildItem.OpenIDConnectInformation info = maybeInfo.get();

                            AutoUrl authorizationUrl = new AutoUrl(
                                    config.oidcOpenIdConnectUrl.orElse(null),
                                    info.getUrlConfigKey(),
                                    "/protocol/openid-connect/auth");

                            AutoUrl refreshUrl = new AutoUrl(
                                    config.oidcOpenIdConnectUrl.orElse(null),
                                    info.getUrlConfigKey(),
                                    "/protocol/openid-connect/token");

                            AutoUrl tokenUrl = new AutoUrl(
                                    config.oidcOpenIdConnectUrl.orElse(null),
                                    info.getUrlConfigKey(),
                                    "/protocol/openid-connect/token/introspect");

                            return new OpenIDConnectSecurityFilter(
                                    config.securitySchemeName,
                                    config.securitySchemeDescription,
                                    authorizationUrl, refreshUrl, tokenUrl);

                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return null;
    }

    private OASFilter getAutoRolesAllowedFilter(String securitySchemeName,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            SmallRyeOpenApiConfig config) {
        if (config.autoAddSecurityRequirement) {
            Map<String, List<String>> rolesAllowedMethodReferences = getRolesAllowedMethodReferences(
                    apiFilteredIndexViewBuildItem);
            if (rolesAllowedMethodReferences != null && !rolesAllowedMethodReferences.isEmpty()) {
                if (securitySchemeName == null) {
                    securitySchemeName = config.securitySchemeName;
                }
                return new AutoRolesAllowedFilter(securitySchemeName, rolesAllowedMethodReferences);
            }
        }
        return null;
    }

    private OASFilter getAutoTagFilter(OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            SmallRyeOpenApiConfig config) {

        if (config.autoAddTags) {

            Map<String, String> classNamesMethodReferences = getClassNamesMethodReferences(apiFilteredIndexViewBuildItem);
            if (classNamesMethodReferences != null && !classNamesMethodReferences.isEmpty()) {
                return new AutoTagFilter(classNamesMethodReferences);
            }
        }
        return null;
    }

    private Map<String, List<String>> getRolesAllowedMethodReferences(
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem) {
        List<AnnotationInstance> rolesAllowedAnnotations = new ArrayList<>();
        for (DotName rolesAllowed : SecurityConstants.ROLES_ALLOWED) {
            rolesAllowedAnnotations.addAll(apiFilteredIndexViewBuildItem.getIndex().getAnnotations(rolesAllowed));
        }
        Map<String, List<String>> methodReferences = new HashMap<>();
        DotName securityRequirement = DotName.createSimple(SecurityRequirement.class.getName());
        for (AnnotationInstance ai : rolesAllowedAnnotations) {
            if (ai.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                MethodInfo method = ai.target().asMethod();
                if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                    String ref = JandexUtil.createUniqueMethodReference(method);
                    methodReferences.put(ref, List.of(ai.value().asStringArray()));
                }
            }
            if (ai.target().kind().equals(AnnotationTarget.Kind.CLASS)) {
                ClassInfo classInfo = ai.target().asClass();
                List<MethodInfo> methods = classInfo.methods();
                for (MethodInfo method : methods) {
                    if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                        String ref = JandexUtil.createUniqueMethodReference(method);
                        methodReferences.put(ref, List.of(ai.value().asStringArray()));
                    }
                }
            }
        }
        return methodReferences;
    }

    private Map<String, String> getClassNamesMethodReferences(OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem) {
        List<AnnotationInstance> openapiAnnotations = new ArrayList<>();
        Set<DotName> allOpenAPIEndpoints = getAllOpenAPIEndpoints();
        for (DotName dotName : allOpenAPIEndpoints) {
            openapiAnnotations.addAll(apiFilteredIndexViewBuildItem.getIndex().getAnnotations(dotName));
        }

        Map<String, String> classNames = new HashMap<>();

        for (AnnotationInstance ai : openapiAnnotations) {
            if (ai.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                MethodInfo method = ai.target().asMethod();
                String ref = JandexUtil.createUniqueMethodReference(method);
                classNames.put(ref, method.declaringClass().simpleName());
            }
        }
        return classNames;
    }

    private boolean isValidOpenAPIMethodForAutoAdd(MethodInfo method, DotName securityRequirement) {
        return isOpenAPIEndpoint(method) && !method.hasAnnotation(securityRequirement)
                && method.declaringClass().classAnnotation(securityRequirement) == null;
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

    private boolean isOpenAPIEndpoint(MethodInfo method) {
        Set<DotName> httpAnnotations = getAllOpenAPIEndpoints();
        for (DotName httpAnnotation : httpAnnotations) {
            if (method.hasAnnotation(httpAnnotation)) {
                return true;
            }
        }
        return false;
    }

    private Set<DotName> getAllOpenAPIEndpoints() {
        Set<DotName> httpAnnotations = new HashSet<>();
        httpAnnotations.addAll(JaxRsConstants.HTTP_METHODS);
        httpAnnotations.addAll(SpringConstants.HTTP_METHODS);
        return httpAnnotations;
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
            List<Result> results = findStaticModels(openApiConfig);
            if (!results.isEmpty()) {
                OpenAPI mergedStaticModel = new OpenAPIImpl();
                for (Result result : results) {
                    try (InputStream is = result.inputStream;
                            OpenApiStaticFile staticFile = new OpenApiStaticFile(is, result.format)) {
                        OpenAPI staticFileModel = io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile);
                        mergedStaticModel = MergeUtil.mergeObjects(mergedStaticModel, staticFileModel);
                    }
                }
                return mergedStaticModel;
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
        String defaultPath = httpRootPathBuildItem.getRootPath();
        if (capabilities.isPresent(Capability.RESTEASY)) {
            extensions.add(new RESTEasyExtension(indexView));
            if (resteasyJaxrsConfig.isPresent()) {
                defaultPath = resteasyJaxrsConfig.get().getRootPath();
            }
        } else if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            extensions.add(new RESTEasyExtension(indexView));
            Optional<String> maybePath = config.getOptionalValue("quarkus.resteasy-reactive.path", String.class);
            if (maybePath.isPresent()) {
                defaultPath = maybePath.get();
            }
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

    private List<Result> findStaticModels(SmallRyeOpenApiConfig openApiConfig) {
        List<Result> results = new ArrayList<>();

        // First check for the file in both META-INF and WEB-INF/classes/META-INF
        results = addStaticModelIfExist(results, Format.YAML, META_INF_OPENAPI_YAML);
        results = addStaticModelIfExist(results, Format.YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML);
        results = addStaticModelIfExist(results, Format.YAML, META_INF_OPENAPI_YML);
        results = addStaticModelIfExist(results, Format.YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YML);
        results = addStaticModelIfExist(results, Format.JSON, META_INF_OPENAPI_JSON);
        results = addStaticModelIfExist(results, Format.JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON);

        // Add any aditional directories if configured
        if (openApiConfig.additionalDocsDirectory.isPresent()) {
            List<Path> additionalStaticDocuments = openApiConfig.additionalDocsDirectory.get();
            for (Path path : additionalStaticDocuments) {
                // Scan all yaml and json files
                try {
                    List<String> filesInDir = getResourceFiles(path.toString());
                    for (String possibleModelFile : filesInDir) {
                        results = addStaticModelIfExist(results, possibleModelFile);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        return results;
    }

    private List<Result> addStaticModelIfExist(List<Result> results, String path) {
        if (path.endsWith(".json")) {
            // Scan a specific json file
            results = addStaticModelIfExist(results, Format.JSON, path);
        } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            // Scan a specific yaml file
            results = addStaticModelIfExist(results, Format.YAML, path);
        }
        return results;
    }

    private List<Result> addStaticModelIfExist(List<Result> results, Format format, String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = cl.getResourceAsStream(path)) {
            if (inputStream != null) {
                results.add(new Result(format, inputStream));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = cl.getResourceAsStream(path)) {
            if (inputStream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String resource;
                    while ((resource = br.readLine()) != null) {
                        filenames.add(path + "/" + resource);
                    }
                }
            }
        }

        return filenames;
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

        Config c = ConfigProvider.getConfig();
        String title = c.getOptionalValue("quarkus.application.name", String.class).orElse("Generated");
        String version = c.getOptionalValue("quarkus.application.version", String.class).orElse("1.0");

        document.archiveName(title);
        document.version(version);

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
