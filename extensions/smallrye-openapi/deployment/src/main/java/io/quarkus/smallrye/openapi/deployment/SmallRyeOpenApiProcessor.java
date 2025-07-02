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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
import io.quarkus.arc.deployment.BuildExclusionsBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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
import io.quarkus.smallrye.openapi.OpenApiFilter;
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
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;
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
        // Add any additional directories if configured
        if (launchMode.getLaunchMode().isDevOrTest() && openApiConfig.additionalDocsDirectory().isPresent()) {
            List<Path> additionalStaticDocuments = openApiConfig.additionalDocsDirectory().get();
            for (Path path : additionalStaticDocuments) {
                // Scan all yaml and json files
                List<String> filesInDir = getResourceFiles(path, outputTargetBuildItem.getOutputDirectory());
                for (String possibleFile : filesInDir) {
                    watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(possibleFile));
                }
            }
        }

        Stream.of("json", "yaml", "yml").forEach(ext -> {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(META_INF_OPENAPI + ext));
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(WEB_INF_CLASSES_META_INF_OPENAPI + ext));
        });
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerAutoSecurityFilter(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            SmallRyeOpenApiConfig openApiConfig,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiRecorder recorder,
            LaunchModeBuildItem launchMode) {
        AutoSecurityFilter autoSecurityFilter = null;

        if (securityConfig(launchMode, openApiConfig::autoAddSecurity)) {
            autoSecurityFilter = getAutoSecurityFilter(securityInformationBuildItems, openApiConfig)
                    .filter(securityFilter -> autoSecurityRuntimeEnabled(securityFilter,
                            () -> hasAutoEndpointSecurity(apiFilteredIndexViewBuildItem, launchMode, openApiConfig)))
                    .orElse(null);
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(OASFilter.class).setRuntimeInit()
                .supplier(recorder.autoSecurityFilterSupplier(autoSecurityFilter)).done());
    }

    static boolean autoSecurityRuntimeEnabled(AutoSecurityFilter autoSecurityFilter,
            Supplier<Boolean> autoRolesAllowedFilterSource) {
        // When the filter is not runtime required, add the security only if there are secured endpoints
        return autoSecurityFilter.runtimeRequired() || autoRolesAllowedFilterSource.get();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerAnnotatedUserDefinedRuntimeFilters(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            OpenApiRecorder recorder) {
        Config config = ConfigProvider.getConfig();

        List<String> userDefinedRuntimeFilters = getUserDefinedRuntimeFilters(config,
                apiFilteredIndexViewBuildItem.getIndex());

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(OpenApiRecorder.UserDefinedRuntimeFilters.class)
                .supplier(recorder.createUserDefinedRuntimeFilters(userDefinedRuntimeFilters))
                .done());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(userDefinedRuntimeFilters.toArray(new String[] {}))
                .reason(getClass().getName()).build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void handler(LaunchModeBuildItem launch,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            OpenApiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
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

        Handler<RoutingContext> handler = recorder.handler();

        Consumer<Route> corsFilter = null;
        // Add CORS filter if the path is not attached to main root
        // as 'http-vertx' only adds CORS filter to http route path
        if (!nonApplicationRootPathBuildItem.isAttachedToMainRouter()) {
            for (FilterBuildItem filterBuildItem : filterBuildItems) {
                if (filterBuildItem.getPriority() == FilterBuildItem.CORS) {
                    corsFilter = recorder.corsFilter(filterBuildItem.toFilter());
                    break;
                }
            }
        }

        routes.produce(RouteBuildItem.newManagementRoute(openApiConfig.path(), MANAGEMENT_ENABLED)
                .withRouteCustomizer(corsFilter)
                .withRoutePathConfigKey("quarkus.smallrye-openapi.path")
                .withRequestHandler(handler)
                .displayOnNotFoundPage("Open API Schema document")
                .asBlockingRoute()
                .build());

        routes.produce(
                RouteBuildItem.newManagementRoute(openApiConfig.path() + ".json", MANAGEMENT_ENABLED)
                        .withRouteCustomizer(corsFilter)
                        .withRequestHandler(handler)
                        .build());

        routes.produce(
                RouteBuildItem.newManagementRoute(openApiConfig.path() + ".yaml", MANAGEMENT_ENABLED)
                        .withRouteCustomizer(corsFilter)
                        .withRequestHandler(handler)
                        .build());

        routes.produce(
                RouteBuildItem.newManagementRoute(openApiConfig.path() + ".yml", MANAGEMENT_ENABLED)
                        .withRouteCustomizer(corsFilter)
                        .withRequestHandler(handler)
                        .build());

        // If management is enabled and swagger-ui is part of management, we need to add CORS so that swagger can hit the endpoint
        if (isManagement(managementBuildTimeConfig, openApiConfig, launch)) {
            Config c = ConfigProvider.getConfig();

            // quarkus.http.cors.enabled=true
            // quarkus.http.cors.origins
            Optional<Boolean> maybeCors = c.getOptionalValue("quarkus.http.cors.enabled", Boolean.class);
            if (!maybeCors.isPresent() || !maybeCors.get().booleanValue()) {
                // We need to set quarkus.http.cors.enabled=true
                systemProperties.produce(new SystemPropertyBuildItem("quarkus.http.cors.enabled", "true"));
            }

            String managementUrl = getManagementRoot(launch, nonApplicationRootPathBuildItem, openApiConfig,
                    managementBuildTimeConfig);

            List<String> origins = c.getOptionalValues("quarkus.http.cors.origins", String.class).orElse(new ArrayList<>());
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
            SmallRyeOpenApiConfig openApiConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        String managementRoot = nonApplicationRootPathBuildItem.resolveManagementPath("/",
                managementBuildTimeConfig, launch, openApiConfig.managementEnabled());

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
        if (config.autoAddOpenApiEndpoint()) {
            addToOpenAPIDefinitionProducer
                    .produce(new AddToOpenAPIDefinitionBuildItem(new AutoAddOpenApiEndpointFilter(config.path())));
        }
    }

    @BuildStep
    void addAutoFilters(BuildProducer<AddToOpenAPIDefinitionBuildItem> addToOpenAPIDefinitionProducer,
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            SmallRyeOpenApiConfig config,
            LaunchModeBuildItem launchModeBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {

        // Add a security scheme from config
        if (config.securityScheme().isPresent()) {
            addToOpenAPIDefinitionProducer
                    .produce(new AddToOpenAPIDefinitionBuildItem(
                            new SecurityConfigFilter(config)));
        } else if (securityConfig(launchModeBuildItem, config::autoAddSecurity)) {
            getAutoSecurityFilter(securityInformationBuildItems, config)
                    .map(AddToOpenAPIDefinitionBuildItem::new)
                    .ifPresent(addToOpenAPIDefinitionProducer::produce);
        }

        // Add operation filter to add tags/descriptions/security requirements
        OASFilter operationFilter = getOperationFilter(apiFilteredIndexViewBuildItem, launchModeBuildItem, config);

        if (operationFilter != null) {
            addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(operationFilter));
        }

        // Add Auto Server based on the current server details
        OASFilter autoServerFilter = getAutoServerFilter(config, false, "Auto generated value");
        if (autoServerFilter != null) {
            addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(autoServerFilter));
        } else if (isManagement(managementBuildTimeConfig, config, launchModeBuildItem)) { // Add server if management is enabled
            OASFilter serverFilter = getAutoServerFilter(config, true, "Auto-added by management interface");
            if (serverFilter != null) {
                addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(serverFilter));
            }
        }
    }

    private List<String> getUserDefinedBuildtimeFilters(IndexView index) {
        return getUserDefinedFilters(index, OpenApiFilter.RunStage.BUILD);
    }

    private List<String> getUserDefinedRuntimeFilters(Config config, IndexView index) {
        List<String> userDefinedFilters = getUserDefinedFilters(index, OpenApiFilter.RunStage.RUN);
        // Also add the MP way
        config.getOptionalValue(OASConfig.FILTER, String.class).ifPresent(userDefinedFilters::add);
        return userDefinedFilters;
    }

    private List<String> getUserDefinedFilters(IndexView index, OpenApiFilter.RunStage stage) {
        EnumSet<OpenApiFilter.RunStage> stages = EnumSet.of(OpenApiFilter.RunStage.BOTH, stage);
        Comparator<Object> comparator = Comparator
                .comparing(x -> ((AnnotationInstance) x).valueWithDefault(index, "priority").asInt())
                .reversed();
        return index
                .getAnnotations(OpenApiFilter.class)
                .stream()
                .filter(ai -> stages.contains(OpenApiFilter.RunStage.valueOf(ai.valueWithDefault(index).asEnum())))
                .sorted(comparator)
                .map(ai -> ai.target().asClass())
                .filter(c -> c.interfaceNames().contains(DotName.createSimple(OASFilter.class.getName())))
                .map(c -> c.name().toString())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isManagement(ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            LaunchModeBuildItem launchModeBuildItem) {
        return managementBuildTimeConfig.enabled() && smallRyeOpenApiConfig.managementEnabled()
                && launchModeBuildItem.getLaunchMode().equals(LaunchMode.DEVELOPMENT);
    }

    private Optional<AutoSecurityFilter> getAutoSecurityFilter(List<SecurityInformationBuildItem> securityInformationBuildItems,
            SmallRyeOpenApiConfig config) {

        if (config.securityScheme().isPresent()) {
            return Optional.empty();
        }

        // Auto add a security from security extension(s)
        return Optional.ofNullable(securityInformationBuildItems)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(securityInfo -> {
                    switch (securityInfo.getSecurityModel()) {
                        case jwt:
                            return new AutoBearerTokenSecurityFilter(
                                    config.securitySchemeName(),
                                    config.securitySchemeDescription(),
                                    config.getValidSecuritySchemeExtensions(),
                                    config.jwtSecuritySchemeValue(),
                                    config.jwtBearerFormat());
                        case oauth2:
                            return new AutoBearerTokenSecurityFilter(
                                    config.securitySchemeName(),
                                    config.securitySchemeDescription(),
                                    config.getValidSecuritySchemeExtensions(),
                                    config.oauth2SecuritySchemeValue(),
                                    config.oauth2BearerFormat());
                        case basic:
                            return new AutoBasicSecurityFilter(
                                    config.securitySchemeName(),
                                    config.securitySchemeDescription(),
                                    config.getValidSecuritySchemeExtensions(),
                                    config.basicSecuritySchemeValue());
                        case oidc:
                            // This needs to be a filter in runtime as the config we use to autoconfigure is in runtime
                            return securityInfo.getOpenIDConnectInformation()
                                    .map(info -> {
                                        AutoUrl openIdConnectUrl = new AutoUrl(
                                                config.oidcOpenIdConnectUrl().orElse(null),
                                                info.getUrlConfigKey(),
                                                "/.well-known/openid-configuration");

                                        return new OpenIDConnectSecurityFilter(
                                                config.securitySchemeName(),
                                                config.securitySchemeDescription(),
                                                config.getValidSecuritySchemeExtensions(),
                                                openIdConnectUrl);
                                    })
                                    .orElse(null);
                        default:
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private boolean hasAutoEndpointSecurity(
            OpenApiFilteredIndexViewBuildItem indexViewBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeOpenApiConfig config) {

        if (securityConfig(launchMode, config::autoAddSecurityRequirement)) {
            Map<String, List<String>> rolesAllowedMethods = Collections.emptyMap();
            List<String> authenticatedMethods = Collections.emptyList();

            rolesAllowedMethods = getRolesAllowedMethodReferences(indexViewBuildItem);

            for (String methodRef : getPermissionsAllowedMethodReferences(indexViewBuildItem)) {
                rolesAllowedMethods.putIfAbsent(methodRef, List.of());
            }

            authenticatedMethods = getAuthenticatedMethodReferences(indexViewBuildItem);

            return !rolesAllowedMethods.isEmpty() || !authenticatedMethods.isEmpty();
        }

        return false;
    }

    private OASFilter getOperationFilter(OpenApiFilteredIndexViewBuildItem indexViewBuildItem,
            LaunchModeBuildItem launchMode,
            SmallRyeOpenApiConfig config) {

        Map<String, ClassAndMethod> classNamesMethods = Collections.emptyMap();
        Map<String, List<String>> rolesAllowedMethods = Collections.emptyMap();
        List<String> authenticatedMethods = Collections.emptyList();

        if (config.autoAddTags() || config.autoAddOperationSummary()) {
            classNamesMethods = getClassNamesMethodReferences(indexViewBuildItem);
        }

        if (securityConfig(launchMode, config::autoAddSecurityRequirement)) {
            rolesAllowedMethods = getRolesAllowedMethodReferences(indexViewBuildItem);

            for (String methodRef : getPermissionsAllowedMethodReferences(indexViewBuildItem)) {
                rolesAllowedMethods.putIfAbsent(methodRef, List.of());
            }

            authenticatedMethods = getAuthenticatedMethodReferences(indexViewBuildItem);
        }

        if (!classNamesMethods.isEmpty() || !rolesAllowedMethods.isEmpty() || !authenticatedMethods.isEmpty()) {
            return new OperationFilter(classNamesMethods, rolesAllowedMethods, authenticatedMethods,
                    config.securitySchemeName(),
                    config.autoAddTags(), config.autoAddOperationSummary(), config.autoAddBadRequestResponse(),
                    isOpenApi_3_1_0_OrGreater(config));
        }

        return null;
    }

    private OASFilter getAutoServerFilter(SmallRyeOpenApiConfig config, boolean defaultFlag, String description) {
        if (config.autoAddServer().orElse(defaultFlag)) {
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

    private Map<String, List<String>> getRolesAllowedMethodReferences(OpenApiFilteredIndexViewBuildItem indexViewBuildItem) {
        IndexView index = indexViewBuildItem.getIndex();
        return SecurityConstants.ROLES_ALLOWED
                .stream()
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .flatMap(t -> getMethods(t, index))
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

    private List<String> getPermissionsAllowedMethodReferences(
            OpenApiFilteredIndexViewBuildItem indexViewBuildItem) {

        FilteredIndexView index = indexViewBuildItem.getIndex();

        return index
                .getAnnotations(DotName.createSimple(PermissionsAllowed.class))
                .stream()
                .flatMap(t -> getMethods(t, index))
                .map(e -> createUniqueMethodReference(e.getKey().classInfo(), e.getKey().method()))
                .distinct()
                .toList();
    }

    private List<String> getAuthenticatedMethodReferences(OpenApiFilteredIndexViewBuildItem indexViewBuildItem) {
        IndexView index = indexViewBuildItem.getIndex();
        return index
                .getAnnotations(DotName.createSimple(Authenticated.class.getName()))
                .stream()
                .flatMap(t -> getMethods(t, index))
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
                    .filter(method -> isValidOpenAPIMethodForAutoAdd(method))
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
        List<MethodInfo> methods = new ArrayList<>();
        methods.addAll(declaringClass.methods());

        // Check if the method overrides a method from an interface
        for (Type interfaceType : declaringClass.interfaceTypes()) {
            ClassInfo interfaceClass = index.getClassByName(interfaceType.name());
            if (interfaceClass != null) {
                for (MethodInfo interfaceMethod : interfaceClass.methods()) {
                    methods.add(interfaceMethod);
                }
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

    private void handleOperation(Operation operation, ClassInfo classInfo, MethodInfo method) {
        String methodRef = createUniqueMethodReference(classInfo, method);
        operation.addExtension(OperationFilter.EXT_METHOD_REF, methodRef);
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
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

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_OPENAPI));

        List<Pattern> urlIgnorePatterns = ignoreStaticDocumentBuildItems.stream()
                .map(IgnoreStaticDocumentBuildItem::getUrlIgnorePattern)
                .toList();

        SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder()
                .withConfig(config)
                .withIndex(index)
                .withApplicationClassLoader(loader)
                .withScannerClassLoader(loader)
                .enableModelReader(true)
                .enableStandardStaticFiles(Boolean.FALSE.equals(smallRyeOpenApiConfig.ignoreStaticDocument()))
                .withResourceLocator(path -> {
                    URL locator = loader.getResource(path);
                    if (locator == null || shouldIgnore(urlIgnorePatterns, locator.toString())) {
                        return null;
                    }
                    return locator;
                })
                .withCustomStaticFile(() -> loadAdditionalDocsModel(smallRyeOpenApiConfig, urlIgnorePatterns,
                        outputTargetBuildItem.getOutputDirectory()))
                .enableAnnotationScan(shouldScanAnnotations(capabilities, index))
                .withScannerFilter(getScannerFilter(capabilities, index))
                .withContextRootResolver(getContextRootResolver(config, capabilities, httpRootPathBuildItem))
                .withTypeConverter(getTypeConverter(index, capabilities))
                .withOperationHandler(this::handleOperation)
                .enableUnannotatedPathParameters(capabilities.isPresent(Capability.RESTEASY_REACTIVE))
                .enableStandardFilter(false)
                .withFilters(openAPIBuildItems.stream()
                        .map(AddToOpenAPIDefinitionBuildItem::getOASFilter)
                        .sorted(Comparator.comparing(filter -> filter.getClass().getName()))
                        .toList());

        getUserDefinedBuildtimeFilters(index).forEach(builder::addFilterName);

        // This should be the final filter to run
        builder.addFilter(new DefaultInfoFilter(config));

        SmallRyeOpenAPI openAPI = builder.build();

        Stream.of(Map.<String, Supplier<String>> entry("JSON", openAPI::toJSON),
                Map.<String, Supplier<String>> entry("YAML", openAPI::toYAML))
                .forEach(format -> {
                    String name = OpenApiConstants.BASE_NAME + format.getKey();
                    byte[] data = format.getValue().get().getBytes(StandardCharsets.UTF_8);
                    resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(name, data));
                    nativeImageResources.produce(new NativeImageResourceBuildItem(name));
                });

        SmallRyeOpenAPI finalOpenAPI;
        SmallRyeOpenAPI storedOpenAPI;

        Supplier<SmallRyeOpenAPI.Builder> filterOnlyBuilder = () -> {
            var runtimeFilterBuilder = SmallRyeOpenAPI.builder()
                    .enableModelReader(false)
                    .enableStandardStaticFiles(false)
                    .enableAnnotationScan(false)
                    .enableStandardFilter(false)
                    .withInitialModel(openAPI.model());

            Optional.ofNullable(getAutoServerFilter(smallRyeOpenApiConfig, true, "Auto generated value"))
                    .ifPresent(runtimeFilterBuilder::addFilter);

            return runtimeFilterBuilder;
        };

        try {
            builder = filterOnlyBuilder.get();
            getUserDefinedRuntimeFilters(config, index).forEach(builder::addFilterName);
            storedOpenAPI = builder.build();
        } catch (Exception e) {
            // Try again without the user-defined runtime filters
            storedOpenAPI = filterOnlyBuilder.get().build();
        }

        finalOpenAPI = storedOpenAPI;

        smallRyeOpenApiConfig.storeSchemaDirectory().ifPresent(storageDir -> {
            try {
                storeGeneratedSchema(storageDir, smallRyeOpenApiConfig.storeSchemaFileName(), outputTargetBuildItem,
                        finalOpenAPI.toJSON(), "json");
                storeGeneratedSchema(storageDir, smallRyeOpenApiConfig.storeSchemaFileName(), outputTargetBuildItem,
                        finalOpenAPI.toYAML(), "yaml");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        openApiDocumentProducer.produce(new OpenApiDocumentBuildItem(toOpenApiDocument(finalOpenAPI), finalOpenAPI));
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

    private InputStream loadAdditionalDocsModel(SmallRyeOpenApiConfig openApiConfig, List<Pattern> ignorePatterns,
            Path target) {
        if (openApiConfig.ignoreStaticDocument()) {
            return null;
        }

        SmallRyeOpenAPI.Builder staticBuilder = SmallRyeOpenAPI.builder()
                .withConfig(ConfigProvider.getConfig())
                .enableModelReader(false)
                .enableStandardStaticFiles(false)
                .enableAnnotationScan(false)
                .enableStandardFilter(false);

        return openApiConfig.additionalDocsDirectory()
                .map(Collection::stream)
                .orElseGet(Stream::empty)
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

    private static boolean isOpenApi_3_1_0_OrGreater(SmallRyeOpenApiConfig config) {
        final String openApiVersion = config.openApiVersion().orElse(null);
        return openApiVersion == null || (!openApiVersion.startsWith("2") && !openApiVersion.startsWith("3.0"));
    }
}
