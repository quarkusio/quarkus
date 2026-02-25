package io.quarkus.smallrye.openapi.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
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
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BuildExclusionsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.server.common.spi.AllowedJaxRsAnnotationPrefixBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.spi.SecurityTransformer;
import io.quarkus.security.spi.SecurityTransformerBuildItem;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.common.deployment.OpenApiDocumentConfig;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.deployment.filter.AutoAddOpenApiEndpointFilter;
import io.quarkus.smallrye.openapi.deployment.filter.AutoServerFilter;
import io.quarkus.smallrye.openapi.deployment.filter.ClassAndMethod;
import io.quarkus.smallrye.openapi.deployment.filter.DefaultInfoFilter;
import io.quarkus.smallrye.openapi.deployment.filter.OperationFilter;
import io.quarkus.smallrye.openapi.deployment.filter.SecurityConfigFilter;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.IgnoreStaticDocumentBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.OpenApiDocumentBuildItem;
import io.quarkus.smallrye.openapi.runtime.OpenApiConfigHelper;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService;
import io.quarkus.smallrye.openapi.runtime.OpenApiRecorder;
import io.quarkus.smallrye.openapi.runtime.filter.AutoBasicSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoBearerTokenSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoUrl;
import io.quarkus.smallrye.openapi.runtime.filter.OpenIDConnectSecurityFilter;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.quarkus.vertx.http.security.AuthorizationPolicy;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.OperationHandler;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.api.constants.SecurityConstants;
import io.smallrye.openapi.api.util.MergeUtil;
import io.smallrye.openapi.jaxrs.JaxRsConstants;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.smallrye.openapi.spring.SpringConstants;
import io.smallrye.openapi.vertx.VertxConstants;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * The main OpenAPI Processor. This will scan for JAX-RS, Spring and Vert.x Annotations, and, if any, add supplied schemas.
 * The result is added to the deployable unit to be loaded at runtime.
 */
public class SmallRyeOpenApiProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.smallrye.openapi");

    private static final String META_INF_OPENAPI = "META-INF/openapi.";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI = "WEB-INF/classes/META-INF/openapi.";

    private static final DotName OPENAPI_SCHEMA = DotName.createSimple(Schema.class.getName());
    private static final DotName OPENAPI_RESPONSE = DotName.createSimple(APIResponse.class.getName());
    private static final DotName OPENAPI_RESPONSES = DotName.createSimple(APIResponses.class.getName());
    private static final DotName OPENAPI_SECURITY_REQUIREMENT = DotName.createSimple(SecurityRequirement.class.getName());

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

    private static final String MANAGEMENT_ENABLED = "quarkus.smallrye-openapi.management.enabled";

    private static final DotName NAME_OPEN_API_FILTER = DotName.createSimple(OpenApiFilter.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_OPENAPI);
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
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(OASFactoryResolver.class.getName()));
    }

    @BuildStep
    void configFiles(BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            SmallRyeOpenApiConfig openApiConfig,
            LaunchModeBuildItem launchMode,
            OutputTargetBuildItem outputTargetBuildItem) {
        // Add any additional directories if configured for each document
        if (launchMode.getLaunchMode().isDevOrTest()) {
            for (OpenApiDocumentConfig documentConfig : openApiConfig.documents().values()) {
                documentConfig.additionalDocsDirectory().ifPresent(additionalStaticDocuments -> {
                    for (Path path : additionalStaticDocuments) {
                        // Scan all yaml and json files
                        List<String> filesInDir = getResourceFiles(path, outputTargetBuildItem.getOutputDirectory());
                        for (String possibleFile : filesInDir) {
                            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(possibleFile));
                        }
                    }
                });
            }
        }

        Stream.of("json", "yaml", "yml").forEach(ext -> {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(META_INF_OPENAPI + ext));
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(WEB_INF_CLASSES_META_INF_OPENAPI + ext));
        });
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(BeanContainerBuildItem.class)
    void prepareDocuments(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiRecorder recorder,
            SmallRyeOpenApiConfig openApiConfig, LaunchModeBuildItem launch,
            Optional<SecurityTransformerBuildItem> securityTransformerBuildItem) {
        Config config = ConfigProvider.getConfig();

        openApiConfig.documents().forEach((documentName, documentConfig) -> {

            Map<OpenApiFilter.RunStage, List<String>> filtersByStage = getUserDefinedFiltersByStage(config,
                    apiFilteredIndexViewBuildItem.getIndex(), documentName);

            AutoSecurityFilter autoSecurityFilter = null;
            if (securityConfig(launch, documentConfig::autoAddSecurity)) {
                autoSecurityFilter = getAutoSecurityFilter(securityInformationBuildItems, documentConfig)
                        .filter(securityFilter -> autoSecurityRuntimeEnabled(securityFilter,
                                () -> hasAutoEndpointSecurity(apiFilteredIndexViewBuildItem, securityTransformerBuildItem)))
                        .orElse(null);
            }

            recorder.prepareDocument(autoSecurityFilter, filtersByStage, documentName);

            List<String> allRuntimeFilters = new ArrayList<>();
            filtersByStage.forEach((stage, filters) -> {
                if (stage != OpenApiFilter.RunStage.BUILD) {
                    allRuntimeFilters.addAll(filters);
                }
            });
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(allRuntimeFilters.toArray(new String[] {}))
                    .reason(getClass().getName()).build());
        });
    }

    private boolean autoSecurityRuntimeEnabled(AutoSecurityFilter autoSecurityFilter,
            Supplier<Boolean> autoRolesAllowedFilterSource) {
        // When the filter is not runtime required, add the security only if there are secured endpoints
        return autoSecurityFilter.runtimeRequired() || autoRolesAllowedFilterSource.get();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerAnnotatedUserDefinedRuntimeFilters(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem,
            OpenApiRecorder recorder) {

        Config config = ConfigProvider.getConfig();
        IndexView index = openApiFilteredIndexViewBuildItem.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(NAME_OPEN_API_FILTER);
        Set<String> userDefinedRuntimeFilters = new LinkedHashSet<>();

        for (AnnotationInstance annotation : annotations) {
            List<String> documentNames = extractDocumentNames(index, annotation);

            for (String documentName : documentNames) {
                Config wrappedConfig = OpenApiConfigHelper.wrap(config, documentName);
                userDefinedRuntimeFilters.addAll(getUserDefinedRuntimeStartupFilters(wrappedConfig, index, documentName));
                userDefinedRuntimeFilters
                        .addAll(getUserDefinedFilters(index, documentName, OpenApiFilter.RunStage.RUNTIME_PER_REQUEST));
            }
        }

        String[] runtimeFilterClassNames = userDefinedRuntimeFilters.toArray(new String[] {});

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(runtimeFilterClassNames)
                .reason(getClass().getName()).build());

        // Make sure the filter beans are kept so they may be loaded programmatically at runtime
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(runtimeFilterClassNames));
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void validateOpenApiFilterStages(BeanArchiveIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(NAME_OPEN_API_FILTER);

        for (AnnotationInstance annotation : annotations) {
            AnnotationValue stagesValue = annotation.valueWithDefault(index, "stages");
            if (stagesValue.asArrayList().isEmpty()) {
                log.warnf(
                        "@OpenApiFilter on '%s' will not be run, since the stages array is set to an empty array (stages = {}).",
                        annotation.target().asClass().name());
            }
        }
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void validateOpenApiFilterDocumentNames(SmallRyeOpenApiConfig config,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem) {
        IndexView index = openApiFilteredIndexViewBuildItem.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(NAME_OPEN_API_FILTER);

        Map<DotName, Set<String>> problematicDocumentNames = new HashMap<>();
        for (AnnotationInstance annotation : annotations) {
            List<String> documentNames = extractDocumentNames(index, annotation);

            for (String documentName : documentNames) {
                if (documentName.equals(OpenApiFilter.DEFAULT_DOCUMENT_NAME)) {
                    continue;
                }
                if (documentName.equals(OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT)) {
                    continue;
                }

                if (config.documents().containsKey(documentName)) {
                    continue;
                }

                problematicDocumentNames.computeIfAbsent(annotation.target().asClass().name(), ignored -> new LinkedHashSet<>())
                        .add(documentName);
            }
        }

        if (!problematicDocumentNames.isEmpty()) {
            Set<String> validDocumentNamesValues = new HashSet<>(config.documents().keySet());
            validDocumentNamesValues.add(OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT);

            String message = """
                    Following instances of the OpenAPIFilter annotation are invalid because of a misconfigured documentNames value.
                    Valid values are: %s
                    """
                    .formatted(validDocumentNamesValues);
            message += problematicDocumentNames.entrySet().stream()
                    .map(entry -> String.format("@OpenAPIFilter '%s' references unknown document names: %s",
                            entry.getKey(),
                            entry.getValue()))
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(message);
        }
    }

    private List<String> extractDocumentNames(IndexView index, AnnotationInstance openApiFilterAnnotation) {

        AnnotationValue annotationValue = openApiFilterAnnotation.valueWithDefault(index, "documentNames");

        List<String> documentNames = new ArrayList<>();
        for (AnnotationValue value : annotationValue.asArrayList()) {
            documentNames.add(value.asString());
        }

        return documentNames;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void handler(LaunchModeBuildItem launch,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            OpenApiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            ShutdownContextBuildItem shutdownContext,
            SmallRyeOpenApiConfig openApiConfig,
            List<FilterBuildItem> filterBuildItems,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
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

        Consumer<Route> corsFilter = null;
        // Add CORS filter if the path is not attached to main root
        // as 'http-vertx' only adds CORS filter to http route path
        if (!nonApplicationRootPathBuildItem.isAttachedToMainRouter()) {
            for (FilterBuildItem filterBuildItem : filterBuildItems) {
                if (filterBuildItem.getPriority() == SecurityHandlerPriorities.CORS) {
                    corsFilter = recorder.corsFilter(filterBuildItem.toFilter());
                    break;
                }
            }
        }

        // Register routes for each configured OpenAPI document
        for (Map.Entry<String, OpenApiDocumentConfig> entry : openApiConfig.documents().entrySet()) {
            String documentName = entry.getKey();
            OpenApiDocumentConfig documentConfig = entry.getValue();

            boolean hasPerRequestFilters = !getUserDefinedFilters(
                    apiFilteredIndexViewBuildItem.getIndex(), documentName, OpenApiFilter.RunStage.RUNTIME_PER_REQUEST)
                    .isEmpty();

            boolean dynamic = documentConfig.alwaysRunFilter() || hasPerRequestFilters;
            Handler<RoutingContext> handler = recorder.handler(documentName, dynamic);

            String managementEnabledKey = MANAGEMENT_ENABLED;

            boolean isDefaultDocument = SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME.equals(documentName);
            String displayName = isDefaultDocument
                    ? "OpenAPI Schema document"
                    : "OpenAPI Schema document: " + documentName;

            routes.produce(RouteBuildItem.newManagementRoute(documentConfig.path(), managementEnabledKey)
                    .withRoutePathConfigKey(
                            isDefaultDocument ? "quarkus.smallrye-openapi.path"
                                    : "quarkus.smallrye-openapi.%s.path".formatted(documentName))
                    .withRouteCustomizer(corsFilter)
                    .withRequestHandler(handler)
                    .displayOnNotFoundPage(displayName)
                    .asBlockingRoute()
                    .build());

            routes.produce(
                    RouteBuildItem.newManagementRoute(documentConfig.path() + ".json", managementEnabledKey)
                            .withRouteCustomizer(corsFilter)
                            .withRequestHandler(handler)
                            .build());

            routes.produce(
                    RouteBuildItem.newManagementRoute(documentConfig.path() + ".yaml", managementEnabledKey)
                            .withRouteCustomizer(corsFilter)
                            .withRequestHandler(handler)
                            .build());

            routes.produce(
                    RouteBuildItem.newManagementRoute(documentConfig.path() + ".yml", managementEnabledKey)
                            .withRouteCustomizer(corsFilter)
                            .withRequestHandler(handler)
                            .build());

        }

        // If management is enabled and swagger-ui is part of management, we need to add CORS so that swagger can hit the endpoint
        if (isManagement(managementBuildTimeConfig, openApiConfig, launch)) {
            Config c = ConfigProvider.getConfig();

            // quarkus.http.cors.enabled=true
            // quarkus.http.cors.origins
            Optional<Boolean> maybeCors = c.getOptionalValue("quarkus.http.cors.enabled", Boolean.class);
            if (maybeCors.isEmpty() || !maybeCors.get()) {
                // We need to set quarkus.http.cors.enabled=true
                systemProperties.produce(new SystemPropertyBuildItem("quarkus.http.cors.enabled", "true"));
            }

            String managementUrl = getManagementRoot(launch, nonApplicationRootPathBuildItem, openApiConfig,
                    managementBuildTimeConfig);

            List<String> origins = c.getOptionalValues("quarkus.http.cors.origins", String.class)
                    .orElse(new ArrayList<>());
            if (!origins.contains(managementUrl)) {
                // We need to set quarkus.http.cors.origins
                origins.add(managementUrl);
                String originConfigValue = String.join(",", origins);
                systemProperties.produce(new SystemPropertyBuildItem("quarkus.http.cors.origins", originConfigValue));
            }
        }
    }

    private boolean securityConfig(
            LaunchModeBuildItem launchMode,
            Supplier<Boolean> securitySetting) {

        if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)) {
            Config config = ConfigProvider.getConfig();
            Optional<Boolean> authEnabled = config.getOptionalValue("quarkus.security.auth.enabled-in-dev-mode", Boolean.class);
            if (authEnabled.isPresent() && authEnabled.get()) {
                return securitySetting.get();
            }
            return false;
        } else {
            return securitySetting.get();
        }
    }

    private String getManagementRoot(LaunchModeBuildItem launch,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeOpenApiConfig documentConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        String managementRoot = nonApplicationRootPathBuildItem.resolveManagementPath("/",
                managementBuildTimeConfig, launch, documentConfig.managementEnabled());

        return managementRoot.split(managementBuildTimeConfig.rootPath())[0];

    }

    @BuildStep
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(OpenApiDocumentService.class)
                .setUnremovable().build());
    }

    @BuildStep
    OpenApiFilteredIndexViewBuildItem smallryeOpenApiIndex(CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildExclusionsBuildItem buildExclusionsBuildItem) {

        CompositeIndex compositeIndex = CompositeIndex.create(
                combinedIndexBuildItem.getIndex(),
                beanArchiveIndexBuildItem.getIndex());
        OpenApiConfig config = OpenApiConfig.fromConfig(ConfigProvider.getConfig());
        Set<DotName> buildTimeClassExclusions = buildExclusionsBuildItem.getExcludedDeclaringClasses()
                .stream()
                .map(DotName::createSimple)
                .collect(Collectors.toSet());

        FilteredIndexView indexView = new FilteredIndexView(compositeIndex, config) {
            @Override
            public boolean accepts(DotName className) {
                if (super.accepts(className)) {
                    return !buildTimeClassExclusions.contains(className);
                }

                return false;
            }
        };

        return new OpenApiFilteredIndexViewBuildItem(indexView);
    }

    @BuildStep
    void addAutoOpenApiEndpointFilter(BuildProducer<AddToOpenAPIDefinitionBuildItem> addToOpenAPIDefinitionProducer,
            SmallRyeOpenApiConfig config) {

        config.documents().forEach((documentName, documentConfig) -> {
            if (documentConfig.autoAddOpenApiEndpoint()) {
                addToOpenAPIDefinitionProducer
                        .produce(new AddToOpenAPIDefinitionBuildItem(
                                new AutoAddOpenApiEndpointFilter(documentConfig.path()), documentName));
            }
        });
    }

    @BuildStep
    void addAutoFilters(BuildProducer<AddToOpenAPIDefinitionBuildItem> addToOpenAPIDefinitionProducer,
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            SmallRyeOpenApiConfig config,
            LaunchModeBuildItem launchModeBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            Optional<SecurityTransformerBuildItem> securityTransformerBuildItem) {

        config.documents().forEach((documentName, documentConfig) -> {

            // Add a security scheme from config
            if (documentConfig.securityScheme().isPresent()) {
                addToOpenAPIDefinitionProducer
                        .produce(new AddToOpenAPIDefinitionBuildItem(
                                new SecurityConfigFilter(documentConfig), documentName));
            } else if (securityConfig(launchModeBuildItem, documentConfig::autoAddSecurity)) {
                getAutoSecurityFilter(securityInformationBuildItems, documentConfig)
                        .map(filter -> new AddToOpenAPIDefinitionBuildItem(filter, documentName))
                        .ifPresent(addToOpenAPIDefinitionProducer::produce);
            }

            // Add operation filter to add tags/descriptions/security requirements
            OASFilter operationFilter = getOperationFilter(apiFilteredIndexViewBuildItem, launchModeBuildItem,
                    securityTransformerBuildItem, documentConfig);

            if (operationFilter != null) {
                addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(operationFilter, documentName));
            }

            // Add Auto Server based on the current server details
            OASFilter autoServerFilter = getAutoServerFilter(documentConfig, false, "Auto generated value");
            if (autoServerFilter != null) {
                addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(autoServerFilter, documentName));
            } else if (isManagement(managementBuildTimeConfig, config, launchModeBuildItem)) { // Add server if management is enabled
                OASFilter serverFilter = getAutoServerFilter(documentConfig, true, "Auto-added by management interface");
                if (serverFilter != null) {
                    addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(serverFilter, documentName));
                }
            }

        });
    }

    private List<String> getUserDefinedRuntimeStartupFilters(Config config, IndexView index, String documentName) {
        @SuppressWarnings("removal")
        List<String> userDefinedFilters = getUserDefinedFilters(index,
                documentName, OpenApiFilter.RunStage.RUNTIME_STARTUP, OpenApiFilter.RunStage.RUN);
        // Also add the MP way
        config.getOptionalValue(OASConfig.FILTER, String.class).ifPresent(userDefinedFilters::add);
        return userDefinedFilters;
    }

    /**
     * Builds a map of all user-defined filters grouped by their resolved {@link OpenApiFilter.RunStage}.
     * The map never contains {@link OpenApiFilter.RunStage#BOTH} as a key; filters annotated with
     * {@code BOTH} are resolved to {@code BUILD} + {@code RUN}.
     */
    @SuppressWarnings("removal")
    private Map<OpenApiFilter.RunStage, List<String>> getUserDefinedFiltersByStage(Config config, IndexView index,
            String documentName) {
        Map<OpenApiFilter.RunStage, List<String>> result = new EnumMap<>(OpenApiFilter.RunStage.class);
        for (OpenApiFilter.RunStage stage : OpenApiFilter.RunStage.values()) {
            if (stage == OpenApiFilter.RunStage.BOTH) {
                continue;
            }
            result.put(stage, getUserDefinedFilters(index, documentName, stage));
        }

        // Also add the MP way
        config.getOptionalValue(OASConfig.FILTER, String.class)
                .ifPresent(filter -> result.get(OpenApiFilter.RunStage.RUN).add(filter));
        return result;
    }

    /**
     * resolves the effective stages from {@link OpenApiFilter#stages()} and {@link OpenApiFilter#value()}.
     *
     * @param ai the OpenApiFilter annotation placed on an OASFilter implementation
     * @param index
     * @return set of the Runstages this OasFilter should run in, never null.
     *         {@link io.quarkus.smallrye.openapi.OpenApiFilter.RunStage#BOTH} will not be present, instead it will be resolved
     *         to {@link io.quarkus.smallrye.openapi.OpenApiFilter.RunStage#BUILD} +
     *         {@link io.quarkus.smallrye.openapi.OpenApiFilter.RunStage#RUN}
     * @deprecated This will be removed once {@link OpenApiFilter#value()} is also removed.
     */
    @Deprecated(since = "3.32", forRemoval = true)
    @SuppressWarnings("removal")
    private Set<OpenApiFilter.RunStage> resolveStages(AnnotationInstance ai, IndexView index) {

        // remember: AnnotationInstance.value does NOT return default values, and instead return null if not explicitly set

        Set<OpenApiFilter.RunStage> runStages = EnumSet.noneOf(OpenApiFilter.RunStage.class);
        AnnotationValue stages = ai.value("stages");
        if (stages != null) {
            for (AnnotationValue sv : stages.asArrayList()) {
                runStages.add(OpenApiFilter.RunStage.valueOf(sv.asEnum()));
            }
        } else {
            AnnotationValue value = ai.value();
            if (value != null) {
                runStages.add(OpenApiFilter.RunStage.valueOf(value.asEnum()));
            } else {
                stages = ai.valueWithDefault(index, "stages");
                for (AnnotationValue sv : stages.asArrayList()) {
                    runStages.add(OpenApiFilter.RunStage.valueOf(sv.asEnum()));
                }
            }
        }

        if (runStages.remove(OpenApiFilter.RunStage.BOTH)) {
            runStages.add(OpenApiFilter.RunStage.BUILD);
            runStages.add(OpenApiFilter.RunStage.RUN);
        }

        return runStages;
    }

    private List<String> getUserDefinedFilters(IndexView index, String documentName,
            OpenApiFilter.RunStage... requestedStages) {
        Comparator<Object> comparator = Comparator
                .comparing(x -> ((AnnotationInstance) x).valueWithDefault(index, "priority").asInt())
                .reversed();

        return index
                .getAnnotations(OpenApiFilter.class)
                .stream()
                .filter(ai -> {
                    Set<OpenApiFilter.RunStage> resolved = resolveStages(ai, index);
                    for (OpenApiFilter.RunStage stage : requestedStages) {
                        if (resolved.contains(stage)) {
                            return true;
                        }
                    }
                    return false;
                })
                .filter(ai -> {
                    List<String> documentNames = extractDocumentNames(index, ai);
                    for (String dn : documentNames) {
                        if (dn.equals(OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT)) {
                            return true;
                        }
                        if (dn.equals(documentName)) {
                            return true;
                        }
                    }

                    return false;
                })
                .sorted(comparator)
                .map(ai -> ai.target().asClass())
                .filter(c -> c.interfaceNames().contains(DotName.createSimple(OASFilter.class.getName())))
                .map(c -> c.name().toString())
                .collect(Collectors.toList());
    }

    private boolean isManagement(ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            LaunchModeBuildItem launchModeBuildItem) {
        return managementBuildTimeConfig.enabled() && smallRyeOpenApiConfig.managementEnabled()
                && launchModeBuildItem.getLaunchMode().equals(LaunchMode.DEVELOPMENT);
    }

    private Optional<AutoSecurityFilter> getAutoSecurityFilter(List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiDocumentConfig documentConfig) {

        if (documentConfig.securityScheme().isPresent()) {
            return Optional.empty();
        }

        // Auto add a security from security extension(s)
        return Optional.ofNullable(securityInformationBuildItems).stream().flatMap(Collection::stream)
                .map(securityInfo -> {
                    return switch (securityInfo.getSecurityModel()) {
                        case jwt -> new AutoBearerTokenSecurityFilter(
                                documentConfig.securitySchemeName(),
                                documentConfig.securitySchemeDescription(),
                                documentConfig.getValidSecuritySchemeExtensions(),
                                documentConfig.jwtSecuritySchemeValue(),
                                documentConfig.jwtBearerFormat());
                        case oauth2 -> new AutoBearerTokenSecurityFilter(
                                documentConfig.securitySchemeName(),
                                documentConfig.securitySchemeDescription(),
                                documentConfig.getValidSecuritySchemeExtensions(),
                                documentConfig.oauth2SecuritySchemeValue(),
                                documentConfig.oauth2BearerFormat());
                        case basic -> new AutoBasicSecurityFilter(
                                documentConfig.securitySchemeName(),
                                documentConfig.securitySchemeDescription(),
                                documentConfig.getValidSecuritySchemeExtensions(),
                                documentConfig.basicSecuritySchemeValue());
                        case oidc ->
                            // This needs to be a filter in runtime as the config we use to autoconfigure is in runtime
                            securityInfo.getOpenIDConnectInformation()
                                    .map(info -> {
                                        AutoUrl openIdConnectUrl = new AutoUrl(
                                                documentConfig.oidcOpenIdConnectUrl().orElse(null),
                                                info.getUrlConfigKey(),
                                                "/.well-known/openid-configuration");

                                        return new OpenIDConnectSecurityFilter(
                                                documentConfig.securitySchemeName(),
                                                documentConfig.securitySchemeDescription(),
                                                documentConfig.getValidSecuritySchemeExtensions(),
                                                openIdConnectUrl);
                                    })
                                    .orElse(null);
                        default -> null;
                    };
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private boolean hasAutoEndpointSecurity(OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            Optional<SecurityTransformerBuildItem> securityTransformerBuildItem) {

        var securityTransformer = createSecurityTransformer(apiFilteredIndexViewBuildItem, securityTransformerBuildItem);
        Map<String, List<String>> authorizedMethods = getAuthorizedMethods(securityTransformer);
        List<String> authenticatedMethods = getAuthenticatedMethodReferences(securityTransformer);

        return !authorizedMethods.isEmpty() || !authenticatedMethods.isEmpty();
    }

    private OASFilter getOperationFilter(OpenApiFilteredIndexViewBuildItem indexViewBuildItem,
            LaunchModeBuildItem launchMode,
            Optional<SecurityTransformerBuildItem> securityTransformerBuildItem,
            OpenApiDocumentConfig documentConfig) {

        Map<String, ClassAndMethod> classNamesMethods = Collections.emptyMap();
        Map<String, List<String>> authorizedMethods = Collections.emptyMap();
        List<String> authenticatedMethods = Collections.emptyList();

        if (documentConfig.autoAddTags() || documentConfig.autoAddOperationSummary()) {
            classNamesMethods = getClassNamesMethodReferences(indexViewBuildItem);
        }

        if (securityConfig(launchMode, documentConfig::autoAddSecurityRequirement)) {
            var securityTransformer = createSecurityTransformer(indexViewBuildItem, securityTransformerBuildItem);
            authorizedMethods = getAuthorizedMethods(securityTransformer);

            authenticatedMethods = getAuthenticatedMethodReferences(securityTransformer);
        }

        if (!classNamesMethods.isEmpty() || !authorizedMethods.isEmpty() || !authenticatedMethods.isEmpty()) {
            return new OperationFilter(classNamesMethods, authorizedMethods, authenticatedMethods,
                    documentConfig.securitySchemeName(),
                    documentConfig.autoAddTags(), documentConfig.autoAddOperationSummary(),
                    documentConfig.autoAddBadRequestResponse(),
                    isOpenApi_3_1_0_OrGreater(documentConfig));
        }

        return null;
    }

    private Map<String, List<String>> getAuthorizedMethods(OpenApiSecurityTransformer securityTransformer) {
        Map<String, List<String>> authorizedMethods = getRolesAllowedMethodReferences(securityTransformer);

        for (String methodRef : getPermissionsAllowedMethodReferences(securityTransformer)) {
            authorizedMethods.putIfAbsent(methodRef, List.of());
        }

        for (String methodRef : getAuthorizationPolicyMethodReferences(securityTransformer)) {
            authorizedMethods.putIfAbsent(methodRef, List.of());
        }
        return authorizedMethods;
    }

    private OASFilter getAutoServerFilter(OpenApiDocumentConfig documentConfig, boolean defaultFlag,
            String description) {
        if (documentConfig.autoAddServer().orElse(defaultFlag)) {
            Config c = ConfigProvider.getConfig();

            String scheme = "http";
            String host = c.getOptionalValue("quarkus.http.host", String.class).orElse("0.0.0.0");
            int port;

            String insecure = c.getOptionalValue("quarkus.http.insecure-requests", String.class).orElse("enabled");
            if (insecure.equalsIgnoreCase("enabled")) {
                port = c.getOptionalValue("quarkus.http.port", Integer.class).orElse(8080);
            } else {
                scheme = "https";
                port = c.getOptionalValue("quarkus.http.ssl-port", Integer.class).orElse(8443);
            }

            return new AutoServerFilter(scheme, host, port, description);
        }
        return null;
    }

    private Map<String, List<String>> getRolesAllowedMethodReferences(OpenApiSecurityTransformer securityTransformer) {
        return SecurityConstants.ROLES_ALLOWED
                .stream()
                .map(securityTransformer::getAnnotations)
                .flatMap(Collection::stream)
                .flatMap(t -> getMethods(t, securityTransformer.getIndex()))
                .collect(Collectors.toMap(
                        e -> createUniqueMethodReference(e.getKey().classInfo(), e.getKey().method()),
                        e -> List.of(e.getValue().value().asStringArray()),
                        (v1, v2) -> {
                            if (!Objects.equals(v1, v2)) {
                                log.warnf("Dropping duplicate annotation, but the values were different; v1: %s, v2: %s", v1,
                                        v2);
                            }
                            return v1;
                        }));
    }

    private List<String> getPermissionsAllowedMethodReferences(OpenApiSecurityTransformer securityTransformer) {
        return securityTransformer
                .getAnnotations(DotName.createSimple(PermissionsAllowed.class))
                .stream()
                .flatMap(t -> getMethods(t, securityTransformer.getIndex()))
                .map(e -> createUniqueMethodReference(e.getKey().classInfo(), e.getKey().method()))
                .distinct()
                .toList();
    }

    private List<String> getAuthorizationPolicyMethodReferences(OpenApiSecurityTransformer securityTransformer) {
        return securityTransformer
                .getAnnotations(DotName.createSimple(AuthorizationPolicy.class))
                .stream()
                .flatMap(t -> getMethods(t, securityTransformer.getIndex()))
                .map(e -> createUniqueMethodReference(e.getKey().classInfo(), e.getKey().method()))
                .distinct()
                .toList();
    }

    private List<String> getAuthenticatedMethodReferences(OpenApiSecurityTransformer securityTransformer) {
        return securityTransformer
                .getAnnotations(DotName.createSimple(Authenticated.class.getName()))
                .stream()
                .flatMap(t -> getMethods(t, securityTransformer.getIndex()))
                .map(e -> createUniqueMethodReference(e.getKey().classInfo(), e.getKey().method()))
                .distinct()
                .toList();
    }

    private static Stream<Map.Entry<ClassAndMethod, AnnotationInstance>> getMethods(AnnotationInstance annotation,
            IndexView index) {
        if (annotation.target().kind() == Kind.METHOD) {
            MethodInfo method = annotation.target().asMethod();

            if (isValidOpenAPIMethodForAutoAdd(method)) {
                return Stream.of(Map.entry(new ClassAndMethod(method.declaringClass(), method), annotation));
            }
        } else if (annotation.target().kind() == Kind.CLASS) {
            ClassInfo classInfo = annotation.target().asClass();
            List<MethodInfo> methods = getMethods(classInfo, index);
            return methods
                    .stream()
                    // drop methods that specify the annotation directly
                    .filter(method -> !method.hasDeclaredAnnotation(annotation.name()))
                    .filter(SmallRyeOpenApiProcessor::isValidOpenAPIMethodForAutoAdd)
                    .map(method -> {
                        final ClassInfo resourceClass;

                        if (method.declaringClass().isInterface()) {
                            /*
                             * smallrye-open-api processes interfaces as the resource class as long as
                             * there is a concrete implementation available. Using the interface method's
                             * declaring class here allows us to match on the hash that will be set by
                             * #handleOperation during scanning.
                             */
                            resourceClass = method.declaringClass();
                        } else {
                            resourceClass = classInfo;
                        }

                        return Map.entry(new ClassAndMethod(resourceClass, method), annotation);
                    });
        }

        return Stream.empty();
    }

    private Map<String, ClassAndMethod> getClassNamesMethodReferences(
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem) {
        FilteredIndexView filteredIndex = apiFilteredIndexViewBuildItem.getIndex();
        List<AnnotationInstance> openapiAnnotations = new ArrayList<>();
        Set<DotName> allOpenAPIEndpoints = getAllOpenAPIEndpoints();
        for (DotName dotName : allOpenAPIEndpoints) {
            openapiAnnotations.addAll(filteredIndex.getAnnotations(dotName));
        }

        Map<String, ClassAndMethod> classNames = new HashMap<>();

        for (AnnotationInstance ai : openapiAnnotations) {
            if (ai.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                MethodInfo method = ai.target().asMethod();
                ClassInfo declaringClass = method.declaringClass();
                Type[] params = method.parameterTypes().toArray(new Type[] {});

                if (Modifier.isInterface(declaringClass.flags())) {
                    addMethodImplementationClassNames(method, params, filteredIndex
                            .getAllKnownImplementors(declaringClass.name()), classNames);
                } else if (Modifier.isAbstract(declaringClass.flags())) {
                    addMethodImplementationClassNames(method, params, filteredIndex
                            .getAllKnownSubclasses(declaringClass.name()), classNames);
                } else {
                    String ref = createUniqueMethodReference(declaringClass, method);
                    classNames.put(ref, new ClassAndMethod(declaringClass, method));
                }
            }
        }
        return classNames;
    }

    void addMethodImplementationClassNames(MethodInfo method, Type[] params, Collection<ClassInfo> classes,
            Map<String, ClassAndMethod> classNames) {
        for (ClassInfo impl : classes) {
            MethodInfo implMethod = impl.method(method.name(), params);

            if (implMethod != null) {
                classNames.put(createUniqueMethodReference(impl, implMethod),
                        new ClassAndMethod(impl, implMethod));
            }

            classNames.put(createUniqueMethodReference(impl, method),
                    new ClassAndMethod(impl, method));
        }
    }

    public static String createUniqueMethodReference(ClassInfo classInfo, MethodInfo methodInfo) {
        return "m" + classInfo.hashCode() + "_" + methodInfo.hashCode();
    }

    private static boolean isValidOpenAPIMethodForAutoAdd(MethodInfo method) {
        return isOpenAPIEndpoint(method) && !method.hasAnnotation(OPENAPI_SECURITY_REQUIREMENT)
                && method.declaringClass().declaredAnnotation(OPENAPI_SECURITY_REQUIREMENT) == null;
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

    private static boolean isOpenAPIEndpoint(MethodInfo method) {
        Set<DotName> httpAnnotations = getAllOpenAPIEndpoints();
        for (DotName httpAnnotation : httpAnnotations) {
            if (method.hasAnnotation(httpAnnotation)) {
                return true;
            }
        }
        return false;
    }

    private static List<MethodInfo> getMethods(ClassInfo declaringClass, IndexView index) {
        List<MethodInfo> methods = new ArrayList<>(declaringClass.methods());

        // Check if the method overrides a method from an interface
        for (Type interfaceType : declaringClass.interfaceTypes()) {
            ClassInfo interfaceClass = index.getClassByName(interfaceType.name());
            if (interfaceClass != null) {
                methods.addAll(interfaceClass.methods());
            }
        }

        DotName superClassName = declaringClass.superName();
        if (superClassName != null) {
            ClassInfo superClass = index.getClassByName(superClassName);

            if (superClass != null) {
                methods.addAll(getMethods(superClass, index));
            }
        }

        return methods;
    }

    private static Set<DotName> getAllOpenAPIEndpoints() {
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
                    reflectiveClass.produce(
                            ReflectiveClassBuildItem.builder(schemaNotClass.asString()).methods().fields().build());
                }

                produceReflectiveHierarchy(reflectiveHierarchy, schema.value(OPENAPI_SCHEMA_ONE_OF), source);
                produceReflectiveHierarchy(reflectiveHierarchy, schema.value(OPENAPI_SCHEMA_ANY_OF), source);
                produceReflectiveHierarchy(reflectiveHierarchy, schema.value(OPENAPI_SCHEMA_ALL_OF), source);
            }
        }
    }

    /**
     * Callback invoked by the smallrye-open-api annotation scanner for each discovered API
     * operation. We use this to set a (private) extension in the OpenAPI model which is then
     * used by the {@link OperationFilter} to match operations with the security and
     * tag information discovered earlier in the build by this class.
     */
    private void addMethodReferenceExtension(Operation operation, ClassInfo classInfo, MethodInfo method) {
        String methodRef = createUniqueMethodReference(classInfo, method);
        operation.addExtension(OperationFilter.EXT_METHOD_REF, methodRef);
    }

    @BuildStep
    public void build(BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<OpenApiDocumentBuildItem> openApiDocumentProducer,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem,
            Capabilities capabilities,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems,
            HttpRootPathBuildItem httpRootPathBuildItem,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            OutputTargetBuildItem outputTargetBuildItem,
            List<IgnoreStaticDocumentBuildItem> ignoreStaticDocumentBuildItems) {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();
        Config config = ConfigProvider.getConfig();

        List<Pattern> urlIgnorePatterns = ignoreStaticDocumentBuildItems.stream()
                .map(IgnoreStaticDocumentBuildItem::getUrlIgnorePattern)
                .toList();

        Map<String, List<AddToOpenAPIDefinitionBuildItem>> openAPIBuildItemsByDocumentName = new HashMap<>();
        openAPIBuildItems.forEach(o -> {
            openAPIBuildItemsByDocumentName.computeIfAbsent(o.getDocumentName(), ignored -> new ArrayList<>()).add(o);
        });

        // Build OpenAPI document for each configured document
        smallRyeOpenApiConfig.documents().forEach((documentName, documentConfig) -> {

            List<OASFilter> oasFilters = Stream.concat(
                    openAPIBuildItemsByDocumentName.getOrDefault(null, Collections.emptyList()).stream(), //
                    openAPIBuildItemsByDocumentName.getOrDefault(documentName, Collections.emptyList()).stream()//
            )
                    .map(AddToOpenAPIDefinitionBuildItem::getOASFilter)
                    .sorted(Comparator.comparing(filter -> filter.getClass().getName()))
                    .toList();

            /*
             * Only add method references if the OperationFilter is enabled. Otherwise,
             * they are not needed.
             */
            OperationHandler operationHandler = oasFilters.stream()
                    .anyMatch(OperationFilter.class::isInstance)
                            ? this::addMethodReferenceExtension
                            : OperationHandler.DEFAULT;

            SmallRyeOpenAPI openAPI = buildOpenApiDocument(
                    documentName,
                    documentConfig,
                    loader,
                    index,
                    config,
                    urlIgnorePatterns,
                    capabilities,
                    oasFilters,
                    httpRootPathBuildItem,
                    operationHandler);

            Map.<String, Supplier<String>> of(//
                    "JSON", openAPI::toJSON, //
                    "YAML", openAPI::toYAML //
            ).forEach((key, value) -> {
                String name = OpenApiConstants.BASE_NAME;
                if (!SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME.equals(documentName)) {
                    name += "-" + documentName;
                }
                name += "." + key;

                byte[] data = value.get().getBytes(StandardCharsets.UTF_8);
                resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(name, data));
                nativeImageResources.produce(new NativeImageResourceBuildItem(name));
            });

            SmallRyeOpenAPI storedOpenAPI = applyRuntimeFilters(openAPI, documentName, documentConfig, config, index);

            // Store schema if configured
            documentConfig.storeSchemaDirectory().ifPresent(storageDir -> {
                try {
                    String documentStoreFileName = documentConfig.storeSchemaFileName();
                    storeGeneratedSchema(storageDir, documentStoreFileName, outputTargetBuildItem,
                            storedOpenAPI.toJSON(), "json");
                    storeGeneratedSchema(storageDir, documentStoreFileName, outputTargetBuildItem,
                            storedOpenAPI.toYAML(), "yaml");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            openApiDocumentProducer
                    .produce(new OpenApiDocumentBuildItem(toOpenApiDocument(storedOpenAPI), storedOpenAPI, documentName));
        });
    }

    private SmallRyeOpenAPI buildOpenApiDocument(
            String documentName,
            OpenApiDocumentConfig documentConfig,
            ClassLoader loader,
            FilteredIndexView index,
            Config config,
            List<Pattern> urlIgnorePatterns,
            Capabilities capabilities,
            List<OASFilter> oasFilters,
            HttpRootPathBuildItem httpRootPathBuildItem,
            OperationHandler operationHandler) {

        Config wrappedConfig = OpenApiConfigHelper.wrap(config, documentName);

        SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder()
                .withConfig(wrappedConfig)
                .withIndex(index)
                .withApplicationClassLoader(loader)
                .withScannerClassLoader(loader)
                .enableModelReader(true)
                .enableStandardStaticFiles(!documentConfig.ignoreStaticDocument())
                .withResourceLocator(path -> {
                    URL locator = loader.getResource(path);
                    if (locator == null || shouldIgnore(urlIgnorePatterns, locator.toString())) {
                        return null;
                    }
                    return locator;
                })
                .withCustomStaticFile(() -> loadAdditionalDocsModel(documentConfig, urlIgnorePatterns, null))
                .enableAnnotationScan(shouldScanAnnotations(capabilities, index))
                .withScannerFilter(getScannerFilter(capabilities, index))
                .withContextRootResolver(getContextRootResolver(config, capabilities, httpRootPathBuildItem))
                .withTypeConverter(getTypeConverter(index, capabilities))
                .withOperationHandler(operationHandler)
                .enableUnannotatedPathParameters(capabilities.isPresent(Capability.RESTEASY_REACTIVE))
                .enableStandardFilter(false)
                .withFilters(oasFilters);

        getUserDefinedFilters(index, documentName, OpenApiFilter.RunStage.BUILD).forEach(builder::addFilterName);

        // This should be the final filter to run
        builder.addFilter(new DefaultInfoFilter(config));

        return builder.build();
    }

    private SmallRyeOpenAPI applyRuntimeFilters(
            SmallRyeOpenAPI openAPI,
            String documentName,
            OpenApiDocumentConfig documentConfig,
            Config config,
            FilteredIndexView index) {

        Supplier<SmallRyeOpenAPI.Builder> filterOnlyBuilder = () -> {
            var runtimeFilterBuilder = SmallRyeOpenAPI.builder()
                    .enableModelReader(false)
                    .enableStandardStaticFiles(false)
                    .enableAnnotationScan(false)
                    .enableStandardFilter(false)
                    .withInitialModel(openAPI.model());

            Optional.ofNullable(getAutoServerFilter(documentConfig, true, "Auto generated value"))
                    .ifPresent(runtimeFilterBuilder::addFilter);

            return runtimeFilterBuilder;
        };

        try {
            SmallRyeOpenAPI.Builder builder = filterOnlyBuilder.get();
            getUserDefinedRuntimeStartupFilters(config, index, documentName).forEach(builder::addFilterName);
            return builder.build();
        } catch (Exception e) {
            // Try again without the user-defined runtime filters
            return filterOnlyBuilder.get().build();
        }
    }

    /**
     * We need to use the deprecated OpenApiDocument as long as
     * OpenApiDocumentBuildItem needs to be produced.
     */
    @SuppressWarnings("deprecation")
    OpenApiDocument toOpenApiDocument(SmallRyeOpenAPI finalOpenAPI) {
        OpenApiDocument output = OpenApiDocument.newInstance();
        output.set(finalOpenAPI.model());
        return output;
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
        reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                .builder(type)
                .ignoreTypePredicate(ResteasyDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                .ignoreFieldPredicate(ResteasyDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                .ignoreMethodPredicate(ResteasyDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                .source(source)
                .build());
    }

    private void storeGeneratedSchema(Path directory, String filename, OutputTargetBuildItem out, String schemaDocument,
            String format)
            throws IOException {
        Path outputDirectory = out.getOutputDirectory();

        if (!directory.isAbsolute() && outputDirectory != null) {
            var baseDir = outputDirectory.getParent();
            // check if outputDirectory is the root of the filesystem
            if (baseDir == null) {
                baseDir = outputDirectory;
            }
            directory = baseDir.resolve(directory);
        }

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path file = directory.resolve(filename + "." + format);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }

        Files.writeString(file, schemaDocument, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("OpenAPI " + format + " saved: " + file.toString());
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
        return !index.getAnnotations(VertxConstants.ROUTE).isEmpty() ||
                !index.getAnnotations(VertxConstants.ROUTE_BASE).isEmpty();
    }

    private InputStream loadAdditionalDocsModel(OpenApiDocumentConfig documentConfig,
            List<Pattern> ignorePatterns, Path target) {
        if (documentConfig.ignoreStaticDocument()) {
            return null;
        }

        SmallRyeOpenAPI.Builder staticBuilder = SmallRyeOpenAPI.builder()
                .withConfig(ConfigProvider.getConfig())
                .enableModelReader(false)
                .enableStandardStaticFiles(false)
                .enableAnnotationScan(false)
                .enableStandardFilter(false);

        return documentConfig.additionalDocsDirectory().stream().flatMap(Collection::stream)
                .map(path -> getResourceFiles(path, target))
                .flatMap(Collection::stream)
                .filter(path -> path.endsWith(".json") || path.endsWith(".yaml") || path.endsWith(".yml"))
                .flatMap(path -> loadResources(path, ignorePatterns))
                .map(stream -> staticBuilder.withCustomStaticFile(() -> stream).build().model())
                .reduce(MergeUtil::merge)
                .map(mergedModel -> staticBuilder
                        .withInitialModel(mergedModel)
                        .withCustomStaticFile(() -> null)
                        .build()
                        .toJSON())
                .map(jsonModel -> new ByteArrayInputStream(jsonModel.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
    }

    private Predicate<String> getScannerFilter(Capabilities capabilities, IndexView index) {
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
        return scanners::contains;
    }

    private Function<Collection<ClassInfo>, String> getContextRootResolver(Config config, Capabilities capabilities,
            HttpRootPathBuildItem httpRootPathBuildItem) {
        String rootPath = httpRootPathBuildItem.getRootPath();
        String appPath = "";

        if (capabilities.isPresent(Capability.RESTEASY)) {
            appPath = config.getOptionalValue("quarkus.resteasy.path", String.class).orElse("");
        } else if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            appPath = config.getOptionalValue("quarkus.rest.path", String.class).orElse("");
        }

        var resolver = new CustomPathExtension(rootPath, appPath);
        return resolver::resolveContextRoot;
    }

    private UnaryOperator<Type> getTypeConverter(IndexView indexView, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.RESTEASY) || capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            return new RESTEasyExtension(indexView)::resolveAsyncType;
        } else {
            return UnaryOperator.identity();
        }
    }

    private Stream<? extends InputStream> loadResources(String path, List<Pattern> ignorePatterns) {
        Spliterator<URL> resources;

        try {
            var resourceEnum = Thread.currentThread().getContextClassLoader().getResources(path).asIterator();
            resources = Spliterators.spliteratorUnknownSize(resourceEnum, Spliterator.IMMUTABLE);
        } catch (IOException ex) {
            throw new UncheckedIOException("Exception processing resources for path " + path, ex);
        }

        return StreamSupport.stream(resources, false)
                .filter(url -> !shouldIgnore(ignorePatterns, url.toString()))
                .map(url -> loadResource(path, url))
                .filter(Objects::nonNull);
    }

    private InputStream loadResource(String path, URL url) {
        Supplier<String> msg = () -> "An error occurred while processing %s for %s".formatted(url, path);

        try {
            return ClassPathUtils.readStream(url, inputStream -> {
                if (inputStream != null) {
                    try {
                        byte[] contents = IoUtil.readBytes(inputStream);
                        return new ByteArrayInputStream(contents);
                    } catch (IOException ioe) {
                        throw new UncheckedIOException(msg.get(), ioe);
                    }
                }

                return null;
            });
        } catch (IOException e) {
            throw new UncheckedIOException(msg.get(), e);
        }
    }

    private boolean shouldIgnore(List<Pattern> ignorePatterns, String url) {
        for (Pattern ignorePattern : ignorePatterns) {
            Matcher matcher = ignorePattern.matcher(url);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    private List<String> getResourceFiles(Path resourcePath, Path target) {
        final String resourceName = ClassPathUtils.toResourceName(resourcePath);
        List<String> filenames = new ArrayList<>();
        // Here we are resolving the resource dir relative to the classes dir and if it does not exist, we fall back to locating the resource dir on the classpath.
        // Although the classes dir should already be on the classpath.
        // In a QuarkusUnitTest the module's classes dir and the test application root could be different directories, is this code here for that reason?
        final Path targetResourceDir = target == null ? null : target.resolve("classes").resolve(resourcePath);
        if (targetResourceDir != null && Files.exists(targetResourceDir)) {
            try (Stream<Path> paths = Files.list(targetResourceDir)) {
                return paths.map(t -> resourceName + "/" + t.getFileName().toString()).toList();
            } catch (IOException e) {
                throw new UncheckedIOException("An error occurred while processing " + resourcePath, e);
            }
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            // QuarkusClassLoader will return a ByteArrayInputStream of directory entry names
            try (InputStream inputStream = cl.getResourceAsStream(resourceName)) {
                if (inputStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                        String resource;
                        while ((resource = br.readLine()) != null) {
                            filenames.add(resourceName + "/" + resource);
                        }
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("An error occurred while processing " + resourcePath, e);
            }
        }
        return filenames;
    }

    private static boolean isOpenApi_3_1_0_OrGreater(OpenApiDocumentConfig documentConfig) {
        final String openApiVersion = documentConfig.openApiVersion().orElse(null);
        return openApiVersion == null || (!openApiVersion.startsWith("2") && !openApiVersion.startsWith("3.0"));
    }

    private static OpenApiSecurityTransformer createSecurityTransformer(OpenApiFilteredIndexViewBuildItem indexViewBuildItem,
            Optional<SecurityTransformerBuildItem> securityTransformerBuildItem) {
        final SecurityTransformer securityTransformer;
        if (securityTransformerBuildItem.isPresent()) {
            // this means that Quarkus Security extension is present and our source of the truth is the SecurityTransformer
            securityTransformer = SecurityTransformerBuildItem.createSecurityTransformer(indexViewBuildItem.getIndex(),
                    securityTransformerBuildItem);
        } else {
            // this mean that Quarkus Security extension is missing, but we still need to consider edge situations
            // like when the OpenApi document is generated for API dependency without Quarkus Security
            securityTransformer = null;
        }
        var index = indexViewBuildItem.getIndex();
        return new OpenApiSecurityTransformer() {
            @Override
            public Collection<AnnotationInstance> getAnnotations(DotName securityAnnotationName) {
                if (securityTransformer != null) {
                    // use the SecurityTransformer because it is more reliable and covers build-time transformations
                    return securityTransformer.getAnnotations(securityAnnotationName);
                }
                return index.getAnnotations(securityAnnotationName);
            }

            @Override
            public IndexView getIndex() {
                return index;
            }
        };
    }

    private interface OpenApiSecurityTransformer {

        Collection<AnnotationInstance> getAnnotations(DotName securityAnnotationName);

        IndexView getIndex();

    }
}
