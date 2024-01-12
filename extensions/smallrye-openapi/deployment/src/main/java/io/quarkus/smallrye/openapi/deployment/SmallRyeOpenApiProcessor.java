package io.quarkus.smallrye.openapi.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
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
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
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
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.deployment.filter.AutoRolesAllowedFilter;
import io.quarkus.smallrye.openapi.deployment.filter.AutoServerFilter;
import io.quarkus.smallrye.openapi.deployment.filter.AutoTagFilter;
import io.quarkus.smallrye.openapi.deployment.filter.SecurityConfigFilter;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.IgnoreStaticDocumentBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.OpenApiDocumentBuildItem;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService;
import io.quarkus.smallrye.openapi.runtime.OpenApiRecorder;
import io.quarkus.smallrye.openapi.runtime.OpenApiRuntimeConfig;
import io.quarkus.smallrye.openapi.runtime.RuntimeOnlyBuilder;
import io.quarkus.smallrye.openapi.runtime.filter.AutoBasicSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoBearerTokenSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoUrl;
import io.quarkus.smallrye.openapi.runtime.filter.OpenIDConnectSecurityFilter;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceConfiguration;
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
import io.vertx.ext.web.Route;
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
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_PRODUCES_STREAMING,
                "application/octet-stream");
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_CONSUMES_STREAMING,
                "application/octet-stream");
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_PRODUCES, "application/json");
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_CONSUMES, "application/json");
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_PRODUCES_PRIMITIVES, "text/plain");
        System.setProperty(io.smallrye.openapi.api.constants.OpenApiConstants.DEFAULT_CONSUMES_PRIMITIVES, "text/plain");
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
    void runtimeOnly(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        // To map from smallrye and mp config to quarkus
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(RuntimeOnlyBuilder.class.getName()));
    }

    @BuildStep
    void configFiles(BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            SmallRyeOpenApiConfig openApiConfig,
            LaunchModeBuildItem launchMode,
            OutputTargetBuildItem outputTargetBuildItem) throws IOException {
        // Add any additional directories if configured
        if (launchMode.getLaunchMode().isDevOrTest() && openApiConfig.additionalDocsDirectory.isPresent()) {
            List<Path> additionalStaticDocuments = openApiConfig.additionalDocsDirectory.get();
            for (Path path : additionalStaticDocuments) {
                // Scan all yaml and json files
                List<String> filesInDir = getResourceFiles(path, outputTargetBuildItem.getOutputDirectory());
                for (String possibleFile : filesInDir) {
                    watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(possibleFile));
                }
            }
        }

        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(META_INF_OPENAPI_YAML));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(WEB_INF_CLASSES_META_INF_OPENAPI_YAML));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(META_INF_OPENAPI_YML));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(WEB_INF_CLASSES_META_INF_OPENAPI_YML));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(META_INF_OPENAPI_JSON));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(WEB_INF_CLASSES_META_INF_OPENAPI_JSON));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerAutoSecurityFilter(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            SmallRyeOpenApiConfig openApiConfig,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiRecorder recorder) {
        OASFilter autoSecurityFilter = null;
        if (openApiConfig.autoAddSecurity) {
            // Only add the security if there are secured endpoints
            OASFilter autoRolesAllowedFilter = getAutoRolesAllowedFilter(openApiConfig.securitySchemeName,
                    apiFilteredIndexViewBuildItem, openApiConfig);
            if (autoRolesAllowedFilter != null) {
                autoSecurityFilter = getAutoSecurityFilter(securityInformationBuildItems, openApiConfig);
            }
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(OASFilter.class).setRuntimeInit()
                .supplier(recorder.autoSecurityFilterSupplier(autoSecurityFilter)).done());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerAnnotatedUserDefinedRuntimeFilters(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            OpenApiRecorder recorder) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        List<String> userDefinedRuntimeFilters = getUserDefinedRuntimeFilters(openApiConfig,
                apiFilteredIndexViewBuildItem.getIndex());

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(OpenApiRecorder.UserDefinedRuntimeFilters.class)
                .supplier(recorder.createUserDefinedRuntimeFilters(userDefinedRuntimeFilters))
                .done());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(userDefinedRuntimeFilters.toArray(new String[] {})).build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void handler(LaunchModeBuildItem launch,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            OpenApiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            OpenApiRuntimeConfig openApiRuntimeConfig,
            ShutdownContextBuildItem shutdownContext,
            SmallRyeOpenApiConfig openApiConfig,
            List<FilterBuildItem> filterBuildItems,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            ManagementInterfaceConfiguration managementInterfaceConfiguration) {
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

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-openapi.management.enabled")
                .routeFunction(openApiConfig.path, corsFilter)
                .routeConfigKey("quarkus.smallrye-openapi.path")
                .handler(handler)
                .displayOnNotFoundPage("Open API Schema document")
                .blockingRoute()
                .build());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-openapi.management.enabled")
                .routeFunction(openApiConfig.path + ".json", corsFilter)
                .handler(handler)
                .build());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-openapi.management.enabled")
                .routeFunction(openApiConfig.path + ".yaml", corsFilter)
                .handler(handler)
                .build());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management("quarkus.smallrye-openapi.management.enabled")
                .routeFunction(openApiConfig.path + ".yml", corsFilter)
                .handler(handler)
                .build());

        // If management is enabled and swagger-ui is part of management, we need to add CORS so that swagger can hit the endpoint
        if (isManagement(managementInterfaceBuildTimeConfig, openApiConfig, launch)) {
            Config c = ConfigProvider.getConfig();

            // quarkus.http.cors=true
            // quarkus.http.cors.origins
            Optional<Boolean> maybeCors = c.getOptionalValue("quarkus.http.cors", Boolean.class);
            if (!maybeCors.isPresent() || !maybeCors.get().booleanValue()) {
                // We need to set quarkus.http.cors=true
                systemProperties.produce(new SystemPropertyBuildItem("quarkus.http.cors", "true"));
            }

            String managementUrl = getManagementRoot(launch, nonApplicationRootPathBuildItem, openApiConfig,
                    managementInterfaceBuildTimeConfig, managementInterfaceConfiguration);

            List<String> origins = c.getOptionalValues("quarkus.http.cors.origins", String.class).orElse(new ArrayList<>());
            if (!origins.contains(managementUrl)) {
                // We need to set quarkus.http.cors.origins
                origins.add(managementUrl);
                String originConfigValue = String.join(",", origins);
                systemProperties.produce(new SystemPropertyBuildItem("quarkus.http.cors.origins", originConfigValue));
            }

        }
    }

    private String getManagementRoot(LaunchModeBuildItem launch,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeOpenApiConfig openApiConfig,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            ManagementInterfaceConfiguration managementInterfaceConfiguration) {
        String managementRoot = nonApplicationRootPathBuildItem.resolveManagementPath("/",
                managementInterfaceBuildTimeConfig, launch, openApiConfig.managementEnabled);

        return managementRoot.split(managementInterfaceBuildTimeConfig.rootPath)[0];

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
    void addAutoFilters(BuildProducer<AddToOpenAPIDefinitionBuildItem> addToOpenAPIDefinitionProducer,
            List<SecurityInformationBuildItem> securityInformationBuildItems,
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem,
            SmallRyeOpenApiConfig config,
            LaunchModeBuildItem launchModeBuildItem,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig) {

        // Add a security scheme from config
        if (config.securityScheme.isPresent()) {
            addToOpenAPIDefinitionProducer
                    .produce(new AddToOpenAPIDefinitionBuildItem(
                            new SecurityConfigFilter(config)));
        } else if (config.autoAddSecurity) {
            OASFilter autoSecurityFilter = getAutoSecurityFilter(securityInformationBuildItems, config);

            if (autoSecurityFilter != null) {
                addToOpenAPIDefinitionProducer
                        .produce(new AddToOpenAPIDefinitionBuildItem(autoSecurityFilter));
            }
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

        // Add Auto Server based on the current server details
        OASFilter autoServerFilter = getAutoServerFilter(config, false, "Auto generated value");
        if (autoServerFilter != null) {
            addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(autoServerFilter));
        } else if (isManagement(managementInterfaceBuildTimeConfig, config, launchModeBuildItem)) { // Add server if management is enabled
            OASFilter serverFilter = getAutoServerFilter(config, true, "Auto-added by management interface");
            if (serverFilter != null) {
                addToOpenAPIDefinitionProducer.produce(new AddToOpenAPIDefinitionBuildItem(serverFilter));
            }
        }
    }

    private List<String> getUserDefinedBuildtimeFilters(OpenApiConfig openApiConfig, IndexView index) {
        return getUserDefinedFilters(openApiConfig, index, OpenApiFilter.RunStage.BUILD);
    }

    private List<String> getUserDefinedRuntimeFilters(OpenApiConfig openApiConfig, IndexView index) {
        List<String> userDefinedFilters = getUserDefinedFilters(openApiConfig, index, OpenApiFilter.RunStage.RUN);
        // Also add the MP way
        String filter = openApiConfig.filter();
        if (filter != null) {
            userDefinedFilters.add(filter);
        }
        return userDefinedFilters;
    }

    private List<String> getUserDefinedFilters(OpenApiConfig openApiConfig, IndexView index, OpenApiFilter.RunStage stage) {
        EnumSet<OpenApiFilter.RunStage> stages = EnumSet.of(OpenApiFilter.RunStage.BOTH, stage);
        Comparator<Object> comparator = Comparator
                .comparing(x -> ((AnnotationInstance) x).valueWithDefault(index, "priority").asInt())
                .reversed();
        return index
                .getAnnotations(OpenApiFilter.class)
                .stream()
                .filter(ai -> stages.contains(OpenApiFilter.RunStage.valueOf(ai.value().asEnum())))
                .sorted(comparator)
                .map(ai -> ai.target().asClass())
                .filter(c -> c.interfaceNames().contains(DotName.createSimple(OASFilter.class.getName())))
                .map(c -> c.name().toString())
                .collect(Collectors.toList());
    }

    private boolean isManagement(ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            LaunchModeBuildItem launchModeBuildItem) {
        return managementInterfaceBuildTimeConfig.enabled && smallRyeOpenApiConfig.managementEnabled
                && launchModeBuildItem.getLaunchMode().equals(LaunchMode.DEVELOPMENT);
    }

    private OASFilter getAutoSecurityFilter(List<SecurityInformationBuildItem> securityInformationBuildItems,
            SmallRyeOpenApiConfig config) {

        // Auto add a security from security extension(s)
        if (config.securityScheme.isEmpty() && securityInformationBuildItems != null
                && !securityInformationBuildItems.isEmpty()) {
            // This needs to be a filter in runtime as the config we use to autoconfigure is in runtime
            for (SecurityInformationBuildItem securityInformationBuildItem : securityInformationBuildItems) {
                SecurityInformationBuildItem.SecurityModel securityModel = securityInformationBuildItem.getSecurityModel();
                switch (securityModel) {
                    case jwt:
                        return new AutoBearerTokenSecurityFilter(
                                config.securitySchemeName,
                                config.securitySchemeDescription,
                                config.getValidSecuritySchemeExtentions(),
                                config.jwtSecuritySchemeValue,
                                config.jwtBearerFormat);
                    case oauth2:
                        return new AutoBearerTokenSecurityFilter(
                                config.securitySchemeName,
                                config.securitySchemeDescription,
                                config.getValidSecuritySchemeExtentions(),
                                config.oauth2SecuritySchemeValue,
                                config.oauth2BearerFormat);
                    case basic:
                        return new AutoBasicSecurityFilter(
                                config.securitySchemeName,
                                config.securitySchemeDescription,
                                config.getValidSecuritySchemeExtentions(),
                                config.basicSecuritySchemeValue);
                    case oidc:
                        return securityInformationBuildItem.getOpenIDConnectInformation()
                                .map(info -> {
                                    AutoUrl openIdConnectUrl = new AutoUrl(
                                            config.oidcOpenIdConnectUrl.orElse(null),
                                            info.getUrlConfigKey(),
                                            "/.well-known/openid-configuration");

                                    return new OpenIDConnectSecurityFilter(
                                            config.securitySchemeName,
                                            config.securitySchemeDescription,
                                            config.getValidSecuritySchemeExtentions(),
                                            openIdConnectUrl);
                                })
                                .orElse(null);
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
            if (securitySchemeName == null) {
                securitySchemeName = config.securitySchemeName;
            }

            Map<String, List<String>> rolesAllowedMethodReferences = getRolesAllowedMethodReferences(
                    apiFilteredIndexViewBuildItem);

            List<String> authenticatedMethodReferences = getAuthenticatedMethodReferences(
                    apiFilteredIndexViewBuildItem);

            if ((rolesAllowedMethodReferences != null && !rolesAllowedMethodReferences.isEmpty())
                    || (authenticatedMethodReferences != null && !authenticatedMethodReferences.isEmpty())) {

                return new AutoRolesAllowedFilter(securitySchemeName, rolesAllowedMethodReferences,
                        authenticatedMethodReferences);
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

    private OASFilter getAutoServerFilter(SmallRyeOpenApiConfig config, boolean defaultFlag, String description) {
        if (config.autoAddServer.orElse(defaultFlag)) {
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
                    String ref = JandexUtil.createUniqueMethodReference(method.declaringClass(), method);
                    methodReferences.put(ref, List.of(ai.value().asStringArray()));
                }
            }
            if (ai.target().kind().equals(AnnotationTarget.Kind.CLASS)) {
                ClassInfo classInfo = ai.target().asClass();
                List<MethodInfo> methods = classInfo.methods();
                for (MethodInfo method : methods) {
                    if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                        String ref = JandexUtil.createUniqueMethodReference(classInfo, method);
                        methodReferences.putIfAbsent(ref, List.of(ai.value().asStringArray()));
                    }
                }
            }
        }
        return methodReferences;
    }

    private List<String> getAuthenticatedMethodReferences(
            OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem) {
        List<AnnotationInstance> authenticatedAnnotations = new ArrayList<>();
        authenticatedAnnotations.addAll(
                apiFilteredIndexViewBuildItem.getIndex().getAnnotations(DotName.createSimple(Authenticated.class.getName())));

        List<String> methodReferences = new ArrayList<>();
        DotName securityRequirement = DotName.createSimple(SecurityRequirement.class.getName());
        for (AnnotationInstance ai : authenticatedAnnotations) {
            if (ai.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                MethodInfo method = ai.target().asMethod();
                if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                    String ref = JandexUtil.createUniqueMethodReference(method.declaringClass(), method);
                    methodReferences.add(ref);
                }
            }
            if (ai.target().kind().equals(AnnotationTarget.Kind.CLASS)) {
                ClassInfo classInfo = ai.target().asClass();
                List<MethodInfo> methods = classInfo.methods();
                for (MethodInfo method : methods) {
                    if (isValidOpenAPIMethodForAutoAdd(method, securityRequirement)) {
                        String ref = JandexUtil.createUniqueMethodReference(classInfo, method);
                        methodReferences.add(ref);
                    }
                }
            }
        }
        return methodReferences;
    }

    private Map<String, String> getClassNamesMethodReferences(OpenApiFilteredIndexViewBuildItem apiFilteredIndexViewBuildItem) {
        FilteredIndexView filteredIndex = apiFilteredIndexViewBuildItem.getIndex();
        List<AnnotationInstance> openapiAnnotations = new ArrayList<>();
        Set<DotName> allOpenAPIEndpoints = getAllOpenAPIEndpoints();
        for (DotName dotName : allOpenAPIEndpoints) {
            openapiAnnotations.addAll(filteredIndex.getAnnotations(dotName));
        }

        Map<String, String> classNames = new HashMap<>();

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
                    String ref = JandexUtil.createUniqueMethodReference(declaringClass, method);
                    classNames.put(ref, declaringClass.simpleName());
                }
            }
        }
        return classNames;
    }

    void addMethodImplementationClassNames(MethodInfo method, Type[] params, Collection<ClassInfo> classes,
            Map<String, String> classNames) {
        for (ClassInfo impl : classes) {
            String simpleClassName = impl.simpleName();
            MethodInfo implMethod = impl.method(method.name(), params);

            if (implMethod != null) {
                classNames.put(JandexUtil.createUniqueMethodReference(impl, implMethod), simpleClassName);
            }

            classNames.put(JandexUtil.createUniqueMethodReference(impl, method), simpleClassName);
        }
    }

    private boolean isValidOpenAPIMethodForAutoAdd(MethodInfo method, DotName securityRequirement) {
        return isOpenAPIEndpoint(method) && !method.hasAnnotation(securityRequirement)
                && method.declaringClass().declaredAnnotation(securityRequirement) == null;
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
                    reflectiveClass.produce(
                            ReflectiveClassBuildItem.builder(schemaNotClass.asString()).methods().fields().build());
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
            BuildProducer<OpenApiDocumentBuildItem> openApiDocumentProducer,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem,
            Capabilities capabilities,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems,
            HttpRootPathBuildItem httpRootPathBuildItem,
            OutputTargetBuildItem out,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            OutputTargetBuildItem outputTargetBuildItem,
            List<IgnoreStaticDocumentBuildItem> ignoreStaticDocumentBuildItems) throws Exception {
        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();

        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_OPENAPI));

        List<Pattern> urlIgnorePatterns = new ArrayList<>();
        for (IgnoreStaticDocumentBuildItem isdbi : ignoreStaticDocumentBuildItems) {
            urlIgnorePatterns.add(isdbi.getUrlIgnorePattern());
        }

        OpenAPI staticModel = generateStaticModel(smallRyeOpenApiConfig, urlIgnorePatterns,
                outputTargetBuildItem.getOutputDirectory(), config, openApiConfig);

        OpenAPI annotationModel;

        if (shouldScanAnnotations(capabilities, index)) {
            annotationModel = generateAnnotationModel(index, capabilities, httpRootPathBuildItem, config, openApiConfig);
        } else {
            annotationModel = new OpenAPIImpl();
        }
        OpenApiDocument finalDocument = loadDocument(staticModel, annotationModel, openAPIBuildItems, index);

        for (Format format : Format.values()) {
            String name = OpenApiConstants.BASE_NAME + format;
            byte[] schemaDocument = OpenApiSerializer.serialize(finalDocument.get(), format).getBytes(StandardCharsets.UTF_8);
            resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(name, schemaDocument));
            nativeImageResources.produce(new NativeImageResourceBuildItem(name));
        }

        OpenApiDocument finalStoredOpenApiDocument = storeDocument(out, smallRyeOpenApiConfig, index, finalDocument.get());
        openApiDocumentProducer.produce(new OpenApiDocumentBuildItem(finalStoredOpenApiDocument));
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

    private OpenAPI generateStaticModel(SmallRyeOpenApiConfig smallRyeOpenApiConfig, List<Pattern> ignorePatterns, Path target,
            Config config, OpenApiConfig openApiConfig)
            throws IOException {

        if (smallRyeOpenApiConfig.ignoreStaticDocument) {
            return null;
        } else {
            List<Result> results = findStaticModels(smallRyeOpenApiConfig, ignorePatterns, target);
            if (!results.isEmpty()) {
                OpenAPI mergedStaticModel = new OpenAPIImpl();
                for (Result result : results) {
                    try (InputStream is = result.inputStream;
                            OpenApiStaticFile staticFile = new OpenApiStaticFile(is, result.format)) {
                        OpenAPI staticFileModel = io.smallrye.openapi.runtime.OpenApiProcessor
                                .modelFromStaticFile(openApiConfig, staticFile);
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
            Config config, OpenApiConfig openApiConfig) {

        List<AnnotationScannerExtension> extensions = new ArrayList<>();

        // Add the RESTEasy extension if the capability is present
        String rootPath = httpRootPathBuildItem.getRootPath();
        String appPath = "";

        if (capabilities.isPresent(Capability.RESTEASY)) {
            extensions.add(new RESTEasyExtension(indexView));
            appPath = config.getOptionalValue("quarkus.resteasy.path", String.class).orElse("");
        } else if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            extensions.add(new RESTEasyExtension(indexView));
            openApiConfig.doAllowNakedPathParameter();
            appPath = config.getOptionalValue("quarkus.resteasy-reactive.path", String.class).orElse("");
        }

        extensions.add(new CustomPathExtension(rootPath, appPath));

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

    private List<Result> findStaticModels(SmallRyeOpenApiConfig openApiConfig, List<Pattern> ignorePatterns, Path target) {
        List<Result> results = new ArrayList<>();

        // First check for the file in both META-INF and WEB-INF/classes/META-INF
        addStaticModelIfExist(results, ignorePatterns, Format.YAML, META_INF_OPENAPI_YAML);
        addStaticModelIfExist(results, ignorePatterns, Format.YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML);
        addStaticModelIfExist(results, ignorePatterns, Format.YAML, META_INF_OPENAPI_YML);
        addStaticModelIfExist(results, ignorePatterns, Format.YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YML);
        addStaticModelIfExist(results, ignorePatterns, Format.JSON, META_INF_OPENAPI_JSON);
        addStaticModelIfExist(results, ignorePatterns, Format.JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON);

        // Add any additional directories if configured
        if (openApiConfig.additionalDocsDirectory.isPresent()) {
            List<Path> additionalStaticDocuments = openApiConfig.additionalDocsDirectory.get();
            for (Path path : additionalStaticDocuments) {
                // Scan all yaml and json files
                try {
                    List<String> filesInDir = getResourceFiles(path, target);
                    for (String possibleModelFile : filesInDir) {
                        addStaticModelIfExist(results, ignorePatterns, possibleModelFile);
                    }
                } catch (IOException ioe) {
                    throw new UncheckedIOException("An error occurred while processing " + path, ioe);
                }
            }
        }

        return results;
    }

    private void addStaticModelIfExist(List<Result> results, List<Pattern> ignorePatterns, String path) {
        if (path.endsWith(".json")) {
            // Scan a specific json file
            addStaticModelIfExist(results, ignorePatterns, Format.JSON, path);
        } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            // Scan a specific yaml file
            addStaticModelIfExist(results, ignorePatterns, Format.YAML, path);
        }
    }

    private void addStaticModelIfExist(List<Result> results, List<Pattern> ignorePatterns, Format format, String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> urls = cl.getResources(path);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                // Check if we should ignore
                String urlAsString = url.toString();
                if (!shouldIgnore(ignorePatterns, urlAsString)) {
                    // Add as static model
                    URLConnection con = url.openConnection();
                    con.setUseCaches(false);
                    try (InputStream inputStream = con.getInputStream()) {
                        if (inputStream != null) {
                            byte[] contents = IoUtil.readBytes(inputStream);

                            results.add(new Result(format, new ByteArrayInputStream(contents)));
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException("An error occurred while processing " + urlAsString + " for " + path,
                                ex);
                    }
                }
            }

        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
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

    private List<String> getResourceFiles(Path resourcePath, Path target) throws IOException {
        final String resourceName = ClassPathUtils.toResourceName(resourcePath);
        List<String> filenames = new ArrayList<>();
        // Here we are resolving the resource dir relative to the classes dir and if it does not exist, we fall back to locating the resource dir on the classpath.
        // Although the classes dir should already be on the classpath.
        // In a QuarkusUnitTest the module's classes dir and the test application root could be different directories, is this code here for that reason?
        final Path targetResourceDir = target == null ? null : target.resolve("classes").resolve(resourcePath);
        if (targetResourceDir != null && Files.exists(targetResourceDir)) {
            try (Stream<Path> paths = Files.list(targetResourceDir)) {
                return paths.map((t) -> {
                    return resourceName + "/" + t.getFileName().toString();
                }).collect(Collectors.toList());
            }
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try (InputStream inputStream = cl.getResourceAsStream(resourceName)) {
                if (inputStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                        String resource;
                        while ((resource = br.readLine()) != null) {
                            filenames.add(resourceName + "/" + resource);
                        }
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
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems, IndexView index) {
        OpenApiDocument document = prepareOpenApiDocument(staticModel, annotationModel, openAPIBuildItems, index);

        Config c = ConfigProvider.getConfig();
        String title = c.getOptionalValue("quarkus.application.name", String.class).orElse("Generated");
        String version = c.getOptionalValue("quarkus.application.version", String.class).orElse("1.0");

        document.archiveName(title);
        document.version(version);

        document.initialize();
        return document;
    }

    private OpenApiDocument storeDocument(OutputTargetBuildItem out,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            IndexView index,
            OpenAPI loadedModel) throws IOException {
        return storeDocument(out, smallRyeOpenApiConfig, index, loadedModel, true);
    }

    private OpenApiDocument storeDocument(OutputTargetBuildItem out,
            SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            IndexView index,
            OpenAPI loadedModel,
            boolean includeRuntimeFilters) throws IOException {

        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        OpenApiDocument document = prepareOpenApiDocument(loadedModel, null, Collections.emptyList(), index);

        if (includeRuntimeFilters) {
            List<String> userDefinedRuntimeFilters = getUserDefinedRuntimeFilters(openApiConfig, index);
            for (String s : userDefinedRuntimeFilters) {
                document.filter(filter(s, index)); // This usually happens at runtime, so when storing we want to filter here too.
            }
        }

        // By default, also add the auto generated server
        OASFilter autoServerFilter = getAutoServerFilter(smallRyeOpenApiConfig, true, "Auto generated value");
        if (autoServerFilter != null) {
            document.filter(autoServerFilter);
        }

        try {
            document.initialize();
        } catch (RuntimeException re) {
            if (includeRuntimeFilters) {
                // This is a Runtime filter, so it might not work at build time. In that case we ignore the filter.
                return storeDocument(out, smallRyeOpenApiConfig, index, loadedModel, false);
            } else {
                throw re;
            }
        }
        // Store the document if needed
        boolean shouldStore = smallRyeOpenApiConfig.storeSchemaDirectory.isPresent();
        if (shouldStore) {
            for (Format format : Format.values()) {
                byte[] schemaDocument = OpenApiSerializer.serialize(document.get(), format).getBytes(StandardCharsets.UTF_8);
                storeGeneratedSchema(smallRyeOpenApiConfig, out, schemaDocument, format);
            }
        }

        return document;
    }

    private OpenApiDocument prepareOpenApiDocument(OpenAPI staticModel,
            OpenAPI annotationModel,
            List<AddToOpenAPIDefinitionBuildItem> openAPIBuildItems,
            IndexView index) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig,
                Thread.currentThread().getContextClassLoader(), index);

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
        // Add user defined Build time filters
        List<String> userDefinedFilters = getUserDefinedBuildtimeFilters(openApiConfig, index);
        for (String filter : userDefinedFilters) {
            document.filter(filter(filter, index));
        }
        return document;
    }

    private OpenApiDocument createDocument(OpenApiConfig openApiConfig) {
        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);
        return document;
    }

    private OASFilter filter(String className, IndexView index) {
        return OpenApiProcessor.getFilter(className, Thread.currentThread().getContextClassLoader(), index);
    }
}
