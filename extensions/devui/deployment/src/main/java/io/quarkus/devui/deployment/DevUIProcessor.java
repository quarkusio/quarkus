package io.quarkus.devui.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.JandexReflection;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.deployment.extension.Codestart;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.deployment.jsonrpc.DevUIDatabindCodec;
import io.quarkus.devui.runtime.DevUIBuildTimeStaticService;
import io.quarkus.devui.runtime.DevUIRecorder;
import io.quarkus.devui.runtime.VertxRouteInfoService;
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.spi.DevUIContent;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeData;
import io.quarkus.devui.spi.buildtime.FooterLogBuildItem;
import io.quarkus.devui.spi.buildtime.StaticContentBuildItem;
import io.quarkus.devui.spi.buildtime.jsonrpc.AbstractJsonRpcMethod;
import io.quarkus.devui.spi.buildtime.jsonrpc.DeploymentJsonRpcMethod;
import io.quarkus.devui.spi.buildtime.jsonrpc.RecordedJsonRpcMethod;
import io.quarkus.devui.spi.buildtime.jsonrpc.RuntimeJsonRpcMethod;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.LibraryLink;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.devui.spi.page.QuteDataPageBuilder;
import io.quarkus.devui.spi.page.SettingPageBuildItem;
import io.quarkus.devui.spi.page.UnlistedPageBuildItem;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.qute.Qute;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResourcesFilter;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Create the HTTP related Dev UI API Points.
 * This includes the JsonRPC Websocket endpoint and the endpoints that deliver the generated and static content.
 *
 * This also find all jsonrpc methods and make them available in the jsonRPC Router
 */
public class DevUIProcessor {
    private static final String FOOTER_LOG_NAMESPACE = "devui-footer-log";

    private static final String DEVUI = "dev-ui";
    private static final String UNDERSCORE = "_";
    private static final String SLASH = "/";
    private static final String SLASH_ALL = SLASH + "*";
    private static final String JSONRPC = "json-rpc-ws";

    private static final String CONSTRUCTOR = "<init>";

    private final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    private static final String JAR = "jar";
    private static final GACT UI_JAR = new GACT("io.quarkus", "quarkus-devui-resources", null, JAR);
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ARTIFACT = "artifact";
    private static final String METADATA = "metadata";
    private static final String KEYWORDS = "keywords";
    private static final String SHORT_NAME = "short-name";
    private static final String GUIDE = "guide";
    private static final String CATEGORIES = "categories";
    private static final String STATUS = "status";
    private static final String BUILT_WITH = "built-with-quarkus-core";
    private static final String CONFIG = "config";
    private static final String EXTENSION_DEPENDENCIES = "extension-dependencies";
    private static final String CAPABILITIES = "capabilities";
    private static final String PROVIDES = "provides";
    private static final String UNLISTED = "unlisted";
    private static final String HIDE = "hide-in-dev-ui";
    private static final String CODESTART = "codestart";
    private static final String LANGUAGES = "languages";
    private static final String ICON_URL = "icon-url";
    private static final String LIB_GA = "lib-ga";
    private final Pattern libGAPattern = Pattern.compile("([^:\\[\\]]+):([^\\[\\]]+)(\\[(.+?)\\])?");

    private static final Logger log = Logger.getLogger(DevUIProcessor.class);

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    void registerDevUiHandlers(
            DevUIConfig devUIConfig,
            BeanContainerBuildItem beanContainer,
            MvnpmBuildItem mvnpmBuildItem,
            List<DevUIRoutesBuildItem> devUIRoutesBuildItems,
            List<StaticContentBuildItem> staticContentBuildItems,
            BuildProducer<RouteBuildItem> routeProducer,
            DevUIRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            HttpRootPathBuildItem httpRootPathBuildItem,
            ShutdownContextBuildItem shutdownContext) throws IOException {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .orderedRoute(DEVUI + SLASH_ALL, -2 * SecurityHandlerPriorities.CORS)
                .handler(recorder.createLocalHostOnlyFilter(devUIConfig.hosts().orElse(null)))
                .build());

        if (devUIConfig.cors().enabled()) {
            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .orderedRoute(DEVUI + SLASH_ALL, -1 * SecurityHandlerPriorities.CORS)
                    .handler(recorder.createDevUICorsFilter(devUIConfig.hosts().orElse(null)))
                    .build());
        }

        // Websocket for JsonRPC comms
        routeProducer.produce(
                nonApplicationRootPathBuildItem
                        .routeBuilder().route(DEVUI + SLASH + JSONRPC)
                        .handler(recorder.devUIWebSocketHandler())
                        .build());

        // Static handler for components
        for (DevUIRoutesBuildItem devUIRoutesBuildItem : devUIRoutesBuildItems) {
            String route = devUIRoutesBuildItem.getPath();

            String path = nonApplicationRootPathBuildItem.resolvePath(route);
            Handler<RoutingContext> uihandler = recorder.uiHandler(
                    devUIRoutesBuildItem.getFinalDestination(),
                    path,
                    devUIRoutesBuildItem.getWebRootConfigurations(),
                    shutdownContext);

            NonApplicationRootPathBuildItem.Builder builder = nonApplicationRootPathBuildItem.routeBuilder()
                    .route(route)
                    .handler(uihandler);

            if (route.endsWith(DEVUI + SLASH)) {
                builder = builder.displayOnNotFoundPage("Dev UI");
                routeProducer.produce(builder.build());
            }

            routeProducer.produce(
                    nonApplicationRootPathBuildItem.routeBuilder().route(route + SLASH_ALL).handler(uihandler).build());
        }

        String basepath = nonApplicationRootPathBuildItem.resolvePath(DEVUI);
        // For static content generated at build time
        Path devUiBasePath = Files.createTempDirectory("quarkus-devui");
        recorder.shutdownTask(shutdownContext, devUiBasePath.toString());

        for (StaticContentBuildItem staticContentBuildItem : staticContentBuildItems) {

            Map<String, String> urlAndPath = new HashMap<>();
            Map<String, String> descriptions = new HashMap<>();
            Map<String, String> mcpDefaultEnabled = new HashMap<>();
            Map<String, String> contentTypes = new HashMap<>();

            List<DevUIContent> content = staticContentBuildItem.getContent();
            for (DevUIContent c : content) {
                String parsedContent = Qute.fmt(new String(c.getTemplate()), c.getData());
                Path tempFile = devUiBasePath
                        .resolve(c.getFileName());
                Files.writeString(tempFile, parsedContent);

                urlAndPath.put(c.getFileName(), tempFile.toString());
                if (c.getDescriptions() != null && !c.getDescriptions().isEmpty()) {
                    descriptions.putAll(c.getDescriptions());
                }
                if (c.getMcpDefaultEnables() != null && !c.getMcpDefaultEnables().isEmpty()) {
                    mcpDefaultEnabled.putAll(c.getMcpDefaultEnables());
                }

                if (c.getContentTypes() != null && !c.getContentTypes().isEmpty()) {
                    contentTypes.putAll(c.getContentTypes());
                }
            }
            Handler<RoutingContext> buildTimeStaticHandler = recorder.buildTimeStaticHandler(beanContainer.getValue(), basepath,
                    urlAndPath, descriptions, mcpDefaultEnabled, contentTypes);

            routeProducer.produce(
                    nonApplicationRootPathBuildItem.routeBuilder().route(DEVUI + SLASH_ALL)
                            .handler(buildTimeStaticHandler)
                            .build());
        }

        Handler<RoutingContext> endpointInfoHandler = recorder.endpointInfoHandler(basepath);

        routeProducer.produce(
                nonApplicationRootPathBuildItem.routeBuilder().route(DEVUI + SLASH + "endpoints" + SLASH + "*")
                        .handler(endpointInfoHandler)
                        .build());

        // For the Vaadin router (So that bookmarks/url refreshes work)
        for (DevUIRoutesBuildItem devUIRoutesBuildItem : devUIRoutesBuildItems) {
            String route = devUIRoutesBuildItem.getPath();
            basepath = nonApplicationRootPathBuildItem.resolvePath(route);
            Handler<RoutingContext> routerhandler = recorder.vaadinRouterHandler(basepath);
            routeProducer.produce(
                    nonApplicationRootPathBuildItem.routeBuilder().route(route + SLASH_ALL).handler(routerhandler).build());
        }

        // Static mvnpm jars
        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath();
        routeProducer.produce(
                nonApplicationRootPathBuildItem.routeBuilder()
                        .route("_static" + SLASH_ALL)
                        .handler(recorder.mvnpmHandler(contextRoot, mvnpmBuildItem.getMvnpmJars()))
                        .build());

        // Redirect /q/dev -> /q/dev-ui
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev")
                .handler(recorder.redirect(contextRoot))
                .build());

        // Redirect naked to welcome if there is no index.html
        if (!hasOwnIndexHtml()) {
            routeProducer.produce(httpRootPathBuildItem.routeBuilder()
                    .orderedRoute(SLASH, Integer.MAX_VALUE)
                    .handler(recorder.redirect(contextRoot, "welcome"))
                    .build());
        }
    }

    private boolean hasOwnIndexHtml() {
        try {
            Enumeration<URL> jarsWithIndexHtml = Thread.currentThread().getContextClassLoader()
                    .getResources("META-INF/resources/index.html");
            return jarsWithIndexHtml.hasMoreElements();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * This makes sure the Runtime JsonRPC Classes for both the internal Dev UI and extensions is available as a bean and on the
     * index.
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexProducer,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(VertxRouteInfoService.class)
                .setUnremovable().build());

        // Make sure all JsonRPC Providers is in the index
        for (JsonRPCProvidersBuildItem jsonRPCProvidersBuildItem : jsonRPCProvidersBuildItems) {

            Class c = jsonRPCProvidersBuildItem.getJsonRPCMethodProviderClass();
            additionalIndexProducer.produce(new AdditionalIndexedClassesBuildItem(c.getName()));
            DotName defaultBeanScope = jsonRPCProvidersBuildItem.getDefaultBeanScope() == null
                    ? BuiltinScope.APPLICATION.getName()
                    : jsonRPCProvidersBuildItem.getDefaultBeanScope();

            additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(c)
                    .setDefaultScope(defaultBeanScope)
                    .setUnremovable().build());
        }

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonRpcRouter.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable().build());

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(DevUIBuildTimeStaticService.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable().build());
    }

    /**
     * This goes through all jsonRPC methods and discover the methods using Jandex
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void findAllJsonRPCMethods(BuildProducer<JsonRPCRuntimeMethodsBuildItem> jsonRPCMethodsProvider,
            BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer,
            LaunchModeBuildItem launchModeBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems,
            DeploymentMethodBuildItem deploymentMethodBuildItem) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        IndexView index = combinedIndexBuildItem.getIndex();

        Map<String, RuntimeJsonRpcMethod> runtimeMethodsMap = new HashMap<>();// All methods to execute against the runtime classpath
        Map<String, RuntimeJsonRpcMethod> runtimeSubscriptionsMap = new HashMap<>();// All subscriptions to execute against the runtime classpath

        DotName descriptionAnnotation = DotName.createSimple(JsonRpcDescription.class);
        DotName devMCPEnableByDefaultAnnotation = DotName.createSimple(DevMCPEnableByDefault.class);

        // Let's use the Jandex index to find all methods
        for (JsonRPCProvidersBuildItem jsonRPCProvidersBuildItem : jsonRPCProvidersBuildItems) {

            Class clazz = jsonRPCProvidersBuildItem.getJsonRPCMethodProviderClass();
            String extension = jsonRPCProvidersBuildItem.getExtensionPathName(curateOutcomeBuildItem);

            ClassInfo classInfo = index.getClassByName(DotName.createSimple(clazz.getName()));
            if (classInfo != null) {// skip if not found
                for (MethodInfo method : classInfo.methods()) {
                    // Ignore constructor, Only allow public methods, Only allow method with response
                    if (!method.name().equals(CONSTRUCTOR) && Modifier.isPublic(method.flags())
                            && method.returnType().kind() != Type.Kind.VOID) {

                        String methodName = extension + UNDERSCORE + method.name();

                        Map<String, AbstractJsonRpcMethod.Parameter> parameters = new LinkedHashMap<>(); // Keep the order
                        for (int i = 0; i < method.parametersCount(); i++) {
                            String description = null;
                            boolean required = true;
                            Type parameterType = method.parameterType(i);
                            if (DotNames.OPTIONAL.equals(parameterType.name())) {
                                required = false;
                                parameterType = parameterType.asParameterizedType().arguments().get(0);
                            }
                            AnnotationInstance jsonRpcDescriptionAnnotation = method.parameters().get(i)
                                    .annotation(descriptionAnnotation);
                            if (jsonRpcDescriptionAnnotation != null) {
                                AnnotationValue descriptionValue = jsonRpcDescriptionAnnotation.value();
                                if (descriptionValue != null && !descriptionValue.asString().isBlank()) {
                                    description = descriptionValue.asString();
                                }
                            }
                            Class<?> parameterClass = toClass(parameterType);
                            String parameterName = method.parameterName(i);
                            parameters.put(parameterName,
                                    new AbstractJsonRpcMethod.Parameter(parameterClass, description, required));
                        }

                        // Look for @JsonRpcUsage annotation
                        EnumSet<Usage> usage = EnumSet.noneOf(Usage.class);
                        AnnotationInstance jsonRpcUsageAnnotation = method.annotation(DotName.createSimple(JsonRpcUsage.class));
                        if (jsonRpcUsageAnnotation != null) {
                            AnnotationInstance[] usageArray = jsonRpcUsageAnnotation.value().asNestedArray();

                            for (AnnotationInstance usageInstance : usageArray) {
                                String usageStr = usageInstance.value().asEnum();
                                usage.add(Usage.valueOf(usageStr));
                            }
                        }

                        // Look for @JsonRpcDescription annotation
                        String description = null;
                        boolean mcpEnabledByDefault = false;
                        AnnotationInstance jsonRpcDescriptionAnnotation = method
                                .annotation(descriptionAnnotation);
                        if (jsonRpcDescriptionAnnotation != null) {
                            AnnotationValue descriptionValue = jsonRpcDescriptionAnnotation.value();
                            if (descriptionValue != null && !descriptionValue.asString().isBlank()) {
                                description = descriptionValue.asString();
                                usage = Usage.devUIandDevMCP();
                            }

                            AnnotationInstance devMCPEnableByDefaultAnnotationInstance = method
                                    .annotation(devMCPEnableByDefaultAnnotation);
                            if (devMCPEnableByDefaultAnnotationInstance != null) {
                                mcpEnabledByDefault = true;
                            }
                        } else {
                            usage = Usage.onlyDevUI();
                        }

                        RuntimeJsonRpcMethod runtimeJsonRpcMethod = new RuntimeJsonRpcMethod(methodName, description,
                                parameters,
                                usage,
                                mcpEnabledByDefault,
                                clazz,
                                method.hasAnnotation(Blocking.class), method.hasAnnotation(NonBlocking.class));

                        // Create list of available methods for the Javascript side.
                        if (method.returnType().name().equals(DotName.createSimple(Multi.class.getName()))) {
                            runtimeSubscriptionsMap.put(methodName, runtimeJsonRpcMethod);
                        } else {
                            runtimeMethodsMap.put(methodName, runtimeJsonRpcMethod);
                        }

                    }
                }
            }
        }

        jsonRPCMethodsProvider.produce(new JsonRPCRuntimeMethodsBuildItem(runtimeMethodsMap, runtimeSubscriptionsMap));

        // Get all names for UI validation
        Set<String> allMethodsNames = Stream
                .<Map<String, ?>> of(runtimeMethodsMap, deploymentMethodBuildItem.getMethods(),
                        deploymentMethodBuildItem.getRecordedMethods())
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet());
        Set<String> allSubscriptionNames = Stream
                .<Map<String, ?>> of(runtimeSubscriptionsMap, deploymentMethodBuildItem.getSubscriptions(),
                        deploymentMethodBuildItem.getRecordedSubscriptions())
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet());

        BuildTimeConstBuildItem methodInfo = new BuildTimeConstBuildItem("devui-jsonrpc");
        if (!allSubscriptionNames.isEmpty()) {
            methodInfo.addBuildTimeData("jsonRPCSubscriptions", allSubscriptionNames);
        }
        if (!allMethodsNames.isEmpty()) {
            methodInfo.addBuildTimeData("jsonRPCMethods", allMethodsNames);
        }
        buildTimeConstProducer.produce(methodInfo);

    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createJsonRpcRouter(DevUIRecorder recorder,
            BeanContainerBuildItem beanContainer,
            JsonRPCRuntimeMethodsBuildItem jsonRPCMethodsBuildItem,
            DeploymentMethodBuildItem deploymentMethodBuildItem) {

        if (jsonRPCMethodsBuildItem != null) {
            Map<String, RuntimeJsonRpcMethod> runtimeMethodsMap = jsonRPCMethodsBuildItem.getRuntimeMethodsMap();
            Map<String, RuntimeJsonRpcMethod> runtimeSubscriptionsMap = jsonRPCMethodsBuildItem.getRuntimeSubscriptionsMap();

            DevConsoleManager.setGlobal(DevUIRecorder.DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY,
                    JsonMapper.Factory.deploymentLinker().createLinkData(new DevUIDatabindCodec.Factory()));

            recorder.createJsonRpcRouter(beanContainer.getValue(),
                    runtimeToJsonRpcMethods(runtimeMethodsMap),
                    runtimeToJsonRpcMethods(runtimeSubscriptionsMap),
                    deploymentToJsonRpcMethods(deploymentMethodBuildItem.getMethods()),
                    deploymentToJsonRpcMethods(deploymentMethodBuildItem.getSubscriptions()),
                    recordedToJsonRpcMethods(deploymentMethodBuildItem.getRecordedMethods()),
                    recordedToJsonRpcMethods(deploymentMethodBuildItem.getRecordedSubscriptions()));
        }
    }

    private Map<String, JsonRpcMethod> runtimeToJsonRpcMethods(Map<String, RuntimeJsonRpcMethod> m) {
        return mapToJsonRpcMethods(m, this::runtimeToJsonRpcMethod);
    }

    private Map<String, JsonRpcMethod> deploymentToJsonRpcMethods(Map<String, DeploymentJsonRpcMethod> m) {
        return mapToJsonRpcMethods(m, this::toJsonRpcMethod);
    }

    private Map<String, JsonRpcMethod> recordedToJsonRpcMethods(Map<String, RecordedJsonRpcMethod> m) {
        return mapToJsonRpcMethods(m, this::recordedToJsonRpcMethod);
    }

    private <T extends AbstractJsonRpcMethod> Map<String, JsonRpcMethod> mapToJsonRpcMethods(
            Map<String, T> input,
            Function<T, JsonRpcMethod> converter) {

        return input.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> converter.apply(e.getValue())));
    }

    private JsonRpcMethod runtimeToJsonRpcMethod(RuntimeJsonRpcMethod i) {
        JsonRpcMethod o = toJsonRpcMethod(i);

        o.setBean(i.getBean());
        o.setIsExplicitlyBlocking(i.isExplicitlyBlocking());
        o.setIsExplicitlyNonBlocking(i.isExplicitlyNonBlocking());

        return o;
    }

    private JsonRpcMethod recordedToJsonRpcMethod(RecordedJsonRpcMethod i) {
        JsonRpcMethod o = toJsonRpcMethod(i);
        o.setRuntimeValue(i.getRuntimeValue());
        return o;
    }

    private JsonRpcMethod toJsonRpcMethod(AbstractJsonRpcMethod i) {
        JsonRpcMethod o = new JsonRpcMethod();

        o.setMethodName(i.getMethodName());
        o.setDescription(i.getDescription());
        o.setUsage(List.copyOf(i.getUsage()));
        o.setMcpEnabledByDefault(i.isMcpEnabledByDefault());
        if (i.hasParameters()) {
            for (Map.Entry<String, AbstractJsonRpcMethod.Parameter> ip : i.getParameters().entrySet()) {
                o.addParameter(ip.getKey(), ip.getValue().getType(), ip.getValue().getDescription(),
                        ip.getValue().isRequired());
            }
        }

        return o;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void processFooterLogs(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            BuildProducer<FooterPageBuildItem> footerPageProducer,
            List<FooterLogBuildItem> footerLogBuildItems) {

        List<BuildTimeActionBuildItem> devServiceLogs = new ArrayList<>();
        List<FooterPageBuildItem> footers = new ArrayList<>();

        for (FooterLogBuildItem footerLogBuildItem : footerLogBuildItems) {
            // Create the Json-RPC service that will stream the log
            String name = footerLogBuildItem.getName().replaceAll(" ", "");

            BuildTimeActionBuildItem devServiceLogActions = new BuildTimeActionBuildItem(FOOTER_LOG_NAMESPACE);
            if (footerLogBuildItem.hasRuntimePublisher()) {
                devServiceLogActions.subscriptionBuilder()
                        .methodName(name + "Log")
                        .description("Streams the " + name + " log")
                        .runtime(footerLogBuildItem.getRuntimePublisher())
                        .build();
            } else {
                devServiceLogActions.subscriptionBuilder()
                        .methodName(name + "Log")
                        .description("Streams the " + name + " log")
                        .function(ignored -> {
                            try {
                                return footerLogBuildItem.getPublisher();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .build();
            }
            devServiceLogs.add(devServiceLogActions);

            // Create the Footer in the Dev UI
            WebComponentPageBuilder footerLogComponent = Page.webComponentPageBuilder().internal()
                    .namespace(FOOTER_LOG_NAMESPACE)
                    .icon("font-awesome-regular:file-lines")
                    .title(capitalizeFirstLetter(footerLogBuildItem.getName()))
                    .metadata("jsonRpcMethodName", footerLogBuildItem.getName() + "Log")
                    .componentLink("qwc-footer-log.js");

            FooterPageBuildItem footerPageBuildItem = new FooterPageBuildItem(FOOTER_LOG_NAMESPACE, footerLogComponent);
            footers.add(footerPageBuildItem);
        }

        buildTimeActionProducer.produce(devServiceLogs);
        footerPageProducer.produce(footers);
    }

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * This build all the pages for dev ui, based on the extension included
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @SuppressWarnings("unchecked")
    void getAllExtensions(List<CardPageBuildItem> cardPageBuildItems,
            List<MenuPageBuildItem> menuPageBuildItems,
            List<FooterPageBuildItem> footerPageBuildItems,
            List<SettingPageBuildItem> settingPageBuildItems,
            List<UnlistedPageBuildItem> unlistedPageBuildItems,
            LaunchModeBuildItem launchModeBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ExtensionsBuildItem> extensionsProducer,
            BuildProducer<WebJarBuildItem> webJarBuildProducer,
            BuildProducer<DevUIWebJarBuildItem> devUIWebJarProducer,
            Capabilities capabilities) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            // produce extension build item as cascade of build steps rely on it
            var emptyExtensionBuildItem = new ExtensionsBuildItem(List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of());
            extensionsProducer.produce(emptyExtensionBuildItem);
            return;
        }

        // First create the static resources for our own internal components
        webJarBuildProducer.produce(WebJarBuildItem.builder()
                .artifactKey(UI_JAR)
                .root(DEVUI + SLASH).build());

        devUIWebJarProducer.produce(new DevUIWebJarBuildItem(UI_JAR, DEVUI));

        final boolean assistantIsAvailable = capabilities.isPresent(Capability.ASSISTANT);

        // Now go through all extensions and check them for active components
        Map<String, CardPageBuildItem> cardPagesMap = getCardPagesMap(curateOutcomeBuildItem, cardPageBuildItems);
        Map<String, MenuPageBuildItem> menuPagesMap = getMenuPagesMap(curateOutcomeBuildItem, menuPageBuildItems);
        Map<String, List<FooterPageBuildItem>> footerPagesMap = getFooterPagesMap(curateOutcomeBuildItem, footerPageBuildItems);
        Map<String, List<SettingPageBuildItem>> settingPagesMap = getSettingPagesMap(curateOutcomeBuildItem,
                settingPageBuildItems);
        Map<String, List<UnlistedPageBuildItem>> unlistedPagesMap = getUnlistedPagesMap(curateOutcomeBuildItem,
                unlistedPageBuildItems);

        final Yaml yaml = new Yaml();
        List<Extension> activeExtensions = new ArrayList<>();
        List<Extension> inactiveExtensions = new ArrayList<>();
        List<Extension> sectionMenuExtensions = new ArrayList<>();
        List<Extension> footerTabExtensions = new ArrayList<>();
        List<Extension> settingTabExtensions = new ArrayList<>();
        List<Extension> unlistedExtensions = new ArrayList<>();

        for (ResolvedDependency runtimeExt : curateOutcomeBuildItem.getApplicationModel()
                .getDependencies(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
            runtimeExt.getContentTree().accept(BootstrapConstants.EXTENSION_METADATA_PATH, extYamlVisit -> {
                if (extYamlVisit == null) {
                    // this could be an exception but previously the code was simply looking for this resource on the classpath
                    log.error("Failed to locate " + BootstrapConstants.EXTENSION_METADATA_PATH + " in "
                            + runtimeExt.toCompactCoords());
                    return;
                }
                final Path extYaml = extYamlVisit.getPath();
                try {
                    Extension extension = new Extension();
                    final String extensionYaml;
                    try (Scanner scanner = new Scanner(Files.newBufferedReader(extYaml, StandardCharsets.UTF_8))) {
                        scanner.useDelimiter("\\A");
                        extensionYaml = scanner.hasNext() ? scanner.next() : null;
                    }
                    if (extensionYaml == null) {
                        // This is a internal extension (like this one, Dev UI)
                        return;
                    }

                    final Map<String, Object> extensionMap = yaml.load(extensionYaml);

                    if (extensionMap.containsKey(NAME)) {

                        Map<String, Object> metaData = (Map<String, Object>) extensionMap.getOrDefault(METADATA, null);
                        if (metaData != null) {
                            boolean isHidden = Boolean.valueOf(String.valueOf(metaData.getOrDefault(HIDE, false)));
                            if (!isHidden) {

                                String namespace = getExtensionNamespace(extensionMap);
                                extension.setNamespace(namespace);
                                extension.setName((String) extensionMap.get(NAME));
                                extension.setDescription((String) extensionMap.getOrDefault(DESCRIPTION, null));
                                extension.setArtifact((String) extensionMap.getOrDefault(ARTIFACT, null));

                                extension.setKeywords((List<String>) metaData.getOrDefault(KEYWORDS, null));
                                extension.setShortName((String) metaData.getOrDefault(SHORT_NAME, null));

                                if (metaData.containsKey(GUIDE)) {
                                    String guide = (String) metaData.get(GUIDE);
                                    try {
                                        extension.setGuide(new URL(guide));
                                    } catch (MalformedURLException mue) {
                                        log.warn("Could not set Guide URL [" + guide + "] for exception [" + namespace + "]");
                                    }
                                }

                                extension.setCategories((List<String>) metaData.getOrDefault(CATEGORIES, null));
                                extension.setStatus(collectionToString(metaData, STATUS));
                                extension.setBuiltWith((String) metaData.getOrDefault(BUILT_WITH, null));
                                extension.setConfigFilter((List<String>) metaData.getOrDefault(CONFIG, null));
                                extension.setExtensionDependencies(
                                        (List<String>) metaData.getOrDefault(EXTENSION_DEPENDENCIES, null));
                                extension.setUnlisted(String.valueOf(metaData.getOrDefault(UNLISTED, false)));

                                if (metaData.containsKey(CAPABILITIES)) {
                                    Map<String, Object> cap = (Map<String, Object>) metaData.get(CAPABILITIES);
                                    extension.setProvidesCapabilities((List<String>) cap.getOrDefault(PROVIDES, null));
                                }

                                if (metaData.containsKey(CODESTART)) {
                                    Map<String, Object> codestartMap = (Map<String, Object>) metaData.get(CODESTART);
                                    if (codestartMap != null) {
                                        Codestart codestart = new Codestart();
                                        codestart.setName((String) codestartMap.getOrDefault(NAME, null));
                                        codestart.setLanguages(listOrString(codestartMap, LANGUAGES));
                                        codestart.setArtifact((String) codestartMap.getOrDefault(ARTIFACT, null));
                                        extension.setCodestart(codestart);
                                    }
                                }

                                if (cardPagesMap.containsKey(namespace) && cardPagesMap.get(namespace).hasPages()) { // Active
                                    CardPageBuildItem cardPageBuildItem = cardPagesMap.get(namespace);

                                    // Add all card links
                                    List<PageBuilder> cardPageBuilders = cardPageBuildItem.getPages();

                                    Map<String, BuildTimeData> buildTimeData = cardPageBuildItem.getBuildTimeData();
                                    for (PageBuilder pageBuilder : cardPageBuilders) {
                                        Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                        if (!page.isAssistantPage() || assistantIsAvailable) {
                                            extension.addCardPage(page);
                                        }
                                    }

                                    // See if there is a custom card component
                                    cardPageBuildItem.getOptionalCard().ifPresent((card) -> {
                                        card.setNamespace(extension.getNamespace());
                                        extension.setCard(card);
                                    });

                                    // See if there is a headless component
                                    String headlessJs = cardPageBuildItem.getHeadlessComponentLink();
                                    if (headlessJs != null) {
                                        extension.setHeadlessComponent(headlessJs);
                                    }

                                    addLogo(extension, cardPageBuildItem, metaData);
                                    addLibraryLinks(extension, cardPageBuildItem, curateOutcomeBuildItem, metaData);

                                    // Also make sure the static resources for that static resource is available
                                    produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                    activeExtensions.add(extension);
                                } else { // Inactive
                                    if (addLogo(extension, cardPagesMap.get(namespace), metaData)) {
                                        // Also make sure the static resources for that static resource is available
                                        produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                    }

                                    addLibraryLinks(extension, cardPagesMap.get(namespace), curateOutcomeBuildItem,
                                            metaData);

                                    inactiveExtensions.add(extension);
                                }

                                // Menus on the sections menu
                                if (menuPagesMap.containsKey(namespace)) {
                                    MenuPageBuildItem menuPageBuildItem = menuPagesMap.get(namespace);
                                    List<PageBuilder> menuPageBuilders = menuPageBuildItem.getPages();

                                    Map<String, BuildTimeData> buildTimeData = menuPageBuildItem.getBuildTimeData();
                                    for (PageBuilder pageBuilder : menuPageBuilders) {
                                        Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                        if (!page.isAssistantPage() || assistantIsAvailable) {
                                            extension.addMenuPage(page);
                                        }
                                    }
                                    // See if there is a headless component
                                    String headlessJs = menuPageBuildItem.getHeadlessComponentLink();
                                    if (headlessJs != null) {
                                        extension.setHeadlessComponent(headlessJs);
                                    }

                                    // Also make sure the static resources for that static resource is available
                                    produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                    sectionMenuExtensions.add(extension);
                                }

                                // Tabs in the footer
                                if (footerPagesMap.containsKey(namespace)) {

                                    List<FooterPageBuildItem> fbis = footerPagesMap.remove(namespace);
                                    for (FooterPageBuildItem footerPageBuildItem : fbis) {
                                        List<PageBuilder> footerPageBuilders = footerPageBuildItem.getPages();

                                        Map<String, BuildTimeData> buildTimeData = footerPageBuildItem.getBuildTimeData();
                                        for (PageBuilder pageBuilder : footerPageBuilders) {
                                            Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                            if (!page.isAssistantPage() || assistantIsAvailable) {
                                                extension.addFooterPage(page);
                                            }
                                        }

                                        // See if there is a headless component
                                        String headlessJs = footerPageBuildItem.getHeadlessComponentLink();
                                        if (headlessJs != null) {
                                            extension.setHeadlessComponent(headlessJs);
                                        }
                                        // Also make sure the static resources for that static resource is available
                                        produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                        footerTabExtensions.add(extension);
                                    }
                                }

                                // Tabs in the settings page
                                if (settingPagesMap.containsKey(namespace)) {

                                    List<SettingPageBuildItem> sbis = settingPagesMap.remove(namespace);
                                    for (SettingPageBuildItem settingPageBuildItem : sbis) {
                                        List<PageBuilder> settingPageBuilders = settingPageBuildItem.getPages();

                                        Map<String, BuildTimeData> buildTimeData = settingPageBuildItem.getBuildTimeData();
                                        for (PageBuilder pageBuilder : settingPageBuilders) {
                                            Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                            if (!page.isAssistantPage() || assistantIsAvailable) {
                                                extension.addSettingPage(page);
                                            }
                                        }
                                        // See if there is a headless component
                                        String headlessJs = settingPageBuildItem.getHeadlessComponentLink();
                                        if (headlessJs != null) {
                                            extension.setHeadlessComponent(headlessJs);
                                        }
                                        // Also make sure the static resources for that static resource is available
                                        produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                        settingTabExtensions.add(extension);
                                    }
                                }

                                // Unlisted pages
                                if (unlistedPagesMap.containsKey(namespace)) {

                                    List<UnlistedPageBuildItem> ubis = unlistedPagesMap.remove(namespace);
                                    for (UnlistedPageBuildItem unlistedPageBuildItem : ubis) {
                                        List<PageBuilder> unlistedPageBuilders = unlistedPageBuildItem.getPages();

                                        Map<String, BuildTimeData> buildTimeData = unlistedPageBuildItem.getBuildTimeData();
                                        for (PageBuilder pageBuilder : unlistedPageBuilders) {
                                            Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                            if (!page.isAssistantPage() || assistantIsAvailable) {
                                                extension.addUnlistedPage(page);
                                            }
                                        }
                                        // See if there is a headless component
                                        String headlessJs = unlistedPageBuildItem.getHeadlessComponentLink();
                                        if (headlessJs != null) {
                                            extension.setHeadlessComponent(headlessJs);
                                        }
                                        // Also make sure the static resources for that static resource is available
                                        produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                        unlistedExtensions.add(extension);
                                    }
                                }
                            }
                        }

                        Collections.sort(activeExtensions, sortingComparator);
                        Collections.sort(inactiveExtensions, sortingComparator);
                    }
                } catch (IOException | RuntimeException e) {
                    // don't abort, just log, to prevent a single extension from breaking entire dev ui
                    log.error("Failed to process extension descriptor " + extYaml.toUri(), e);
                }
            });
        }

        // Also add footers for extensions that might not have a runtime
        if (!footerPagesMap.isEmpty()) {
            for (Map.Entry<String, List<FooterPageBuildItem>> footer : footerPagesMap.entrySet()) {
                List<FooterPageBuildItem> fbis = footer.getValue();
                for (FooterPageBuildItem footerPageBuildItem : fbis) {

                    Extension deploymentOnlyExtension = new Extension();
                    deploymentOnlyExtension.setName(footer.getKey());
                    deploymentOnlyExtension.setNamespace(FOOTER_LOG_NAMESPACE);

                    List<PageBuilder> footerPageBuilders = footerPageBuildItem.getPages();

                    for (PageBuilder pageBuilder : footerPageBuilders) {
                        pageBuilder.namespace(deploymentOnlyExtension.getNamespace());
                        pageBuilder.extension(deploymentOnlyExtension.getName());
                        Page page = pageBuilder.build();
                        deploymentOnlyExtension.addFooterPage(page);
                    }

                    footerTabExtensions.add(deploymentOnlyExtension);
                }
            }
        }

        // Also add setting for extensions that might not have a runtime
        if (!settingPagesMap.isEmpty()) {
            for (Map.Entry<String, List<SettingPageBuildItem>> setting : settingPagesMap.entrySet()) {
                List<SettingPageBuildItem> sbis = setting.getValue();
                for (SettingPageBuildItem settingPageBuildItem : sbis) {

                    Extension deploymentOnlyExtension = new Extension();
                    deploymentOnlyExtension.setName(setting.getKey());

                    List<PageBuilder> settingPageBuilders = settingPageBuildItem.getPages();

                    for (PageBuilder pageBuilder : settingPageBuilders) {
                        pageBuilder.namespace(deploymentOnlyExtension.getNamespace());
                        pageBuilder.extension(deploymentOnlyExtension.getName());
                        Page page = pageBuilder.build();
                        deploymentOnlyExtension.addSettingPage(page);
                    }

                    settingTabExtensions.add(deploymentOnlyExtension);
                }
            }
        }

        // Also add unlisting pages for extensions that might not have a runtime
        if (!unlistedPagesMap.isEmpty()) {
            for (Map.Entry<String, List<UnlistedPageBuildItem>> setting : unlistedPagesMap.entrySet()) {
                List<UnlistedPageBuildItem> ubis = setting.getValue();
                for (UnlistedPageBuildItem unlistedPageBuildItem : ubis) {

                    Extension deploymentOnlyExtension = new Extension();
                    deploymentOnlyExtension.setName(setting.getKey());

                    List<PageBuilder> unlistedPageBuilders = unlistedPageBuildItem.getPages();

                    for (PageBuilder pageBuilder : unlistedPageBuilders) {
                        pageBuilder.namespace(deploymentOnlyExtension.getNamespace());
                        pageBuilder.extension(deploymentOnlyExtension.getName());
                        Page page = pageBuilder.build();
                        deploymentOnlyExtension.addUnlistedPage(page);
                    }

                    unlistedExtensions.add(deploymentOnlyExtension);
                }
            }
        }

        extensionsProducer.produce(
                new ExtensionsBuildItem(activeExtensions, inactiveExtensions, sectionMenuExtensions, footerTabExtensions,
                        settingTabExtensions, unlistedExtensions));
    }

    private void addLibraryLinks(Extension extension, CardPageBuildItem cardPageBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem, Map<String, Object> metaData) {
        if (cardPageBuildItem != null && !cardPageBuildItem.hasLibraryVersions() && !metaData.containsKey(LIB_GA)) {
            return;
        }

        // Build a lookup map once
        Map<String, String> versionMap = curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                .collect(Collectors.toMap(
                        rd -> rd.getGroupId() + ":" + rd.getArtifactId(),
                        ResolvedDependency::getVersion,
                        (existing, replacement) -> existing // keep the first one
                ));

        if (cardPageBuildItem != null && cardPageBuildItem.hasLibraryVersions()) {
            for (LibraryLink lib : cardPageBuildItem.getLibraryVersions()) {
                String key = lib.getGroupId() + ":" + lib.getArtifactId();
                String version = versionMap.get(key);
                if (version != null) {
                    lib.setVersion(version);
                    extension.addLibraryLink(lib);
                }
            }
        }

        if (metaData.containsKey(LIB_GA) && !extension.hasLibraryLinks()) {
            String libGA = (String) metaData.get(LIB_GA);
            Matcher matcher = libGAPattern.matcher(libGA);

            if (matcher.matches()) {

                String groupId = matcher.group(1);
                String artifactId = matcher.group(2);
                URL url = null;
                if (matcher.group(4) != null) {
                    try {
                        url = URI.create(matcher.group(4)).toURL();
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                    }
                }

                String version = versionMap.get(groupId + ":" + artifactId);
                if (version != null) {
                    LibraryLink l = new LibraryLink(groupId, artifactId, artifactId, url);
                    l.setVersion(version);
                    extension.addLibraryLink(l);
                }
            }
        }

    }

    private boolean addLogo(Extension extension, CardPageBuildItem cardPageBuildItem, Map<String, Object> metaData) {
        if (cardPageBuildItem != null && cardPageBuildItem.hasLogo()) {
            extension.setLogo(cardPageBuildItem.getDarkLogo(), cardPageBuildItem.getLightLogo());
            return true;
        } else if (metaData.containsKey(ICON_URL)) {
            String iconUrl = (String) metaData.get(ICON_URL);
            extension.setLogo(iconUrl, iconUrl);
            return true;
        }
        return false;
    }

    private String collectionToString(Map<String, Object> metaData, String key) {
        Object value = metaData.getOrDefault(key, null);
        if (value == null) {
            return null;
        } else if (String.class.isAssignableFrom(value.getClass())) {
            return (String) value;
        } else if (List.class.isAssignableFrom(value.getClass())) {
            List values = (List) value;
            return (String) values.stream()
                    .map(n -> String.valueOf(n))
                    .collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    private List<String> listOrString(Map<String, Object> metaData, String key) {
        Object value = metaData.getOrDefault(key, null);
        if (value == null) {
            return null;
        } else if (String.class.isAssignableFrom(value.getClass())) {
            return List.of((String) value);
        } else if (List.class.isAssignableFrom(value.getClass())) {
            return (List) value;
        }
        return List.of(String.valueOf(value));
    }

    private void produceResources(ResolvedDependency runtimeExt,
            BuildProducer<WebJarBuildItem> webJarBuildProducer,
            BuildProducer<DevUIWebJarBuildItem> devUIWebJarProducer) {

        String namespace = getNamespace(runtimeExt.getKey());
        if (namespace.isEmpty()) {
            namespace = "devui";
        }
        String buildTimeDataImport = namespace + "-data";

        final GACT deploymentKey = getDeploymentKey(runtimeExt);
        webJarBuildProducer.produce(WebJarBuildItem.builder()
                .artifactKey(deploymentKey)
                .root(DEVUI + SLASH)
                .filter(new WebJarResourcesFilter() {
                    @Override
                    public WebJarResourcesFilter.FilterResult apply(String fileName, InputStream file) throws IOException {
                        if (fileName.endsWith(".js")) {
                            String content = new String(file.readAllBytes(), StandardCharsets.UTF_8);
                            content = content.replaceAll("build-time-data", buildTimeDataImport);
                            return new WebJarResourcesFilter.FilterResult(
                                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true);
                        }

                        return new WebJarResourcesFilter.FilterResult(file, false);
                    }
                })
                .build());

        devUIWebJarProducer.produce(new DevUIWebJarBuildItem(deploymentKey, DEVUI));
    }

    private static GACT getDeploymentKey(ResolvedDependency runtimeExt) {
        return runtimeExt.getContentTree().apply(BootstrapConstants.DESCRIPTOR_PATH, extPropsVisit -> {
            if (extPropsVisit == null) {
                throw new RuntimeException("Failed to locate " + BootstrapConstants.DESCRIPTOR_PATH
                        + " in " + runtimeExt.toCompactCoords());
            }
            final Properties props = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(extPropsVisit.getPath())) {
                props.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + extPropsVisit.getUrl(), e);
            }
            final String deploymentCoords = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            if (deploymentCoords == null) {
                throw new RuntimeException(
                        "Failed to locate " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT + " in " + extPropsVisit.getUrl());
            }
            var coords = GACTV.fromString(deploymentCoords);
            return new GACT(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType());
        });
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createAllRoutes(WebJarResultsBuildItem webJarResultsBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            List<DevUIWebJarBuildItem> devUIWebJarBuiltItems,
            BuildProducer<DevUIRoutesBuildItem> devUIRoutesProducer) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        for (DevUIWebJarBuildItem devUIWebJarBuiltItem : devUIWebJarBuiltItems) {
            WebJarResultsBuildItem.WebJarResult result = webJarResultsBuildItem
                    .byArtifactKey(devUIWebJarBuiltItem.getArtifactKey());
            if (result != null) {
                String namespace = getNamespace(devUIWebJarBuiltItem.getArtifactKey());
                devUIRoutesProducer.produce(new DevUIRoutesBuildItem(namespace, devUIWebJarBuiltItem.getPath(),
                        result.getFinalDestination(), result.getWebRootConfigurations()));
            }
        }
    }

    private String getNamespace(ArtifactKey artifactKey) {
        String namespace = artifactKey.getArtifactId();

        if (namespace.equals("quarkus-devui-resources")) {
            // Internal
            namespace = "";
        } else if (namespace.endsWith("-deployment")) {
            int end = namespace.lastIndexOf("-");
            namespace = namespace.substring(0, end);
        }
        return namespace;
    }

    private Page buildFinalPage(PageBuilder pageBuilder, Extension extension, Map<String, BuildTimeData> buildTimeData) {
        pageBuilder.namespace(extension.getNamespace());
        pageBuilder.extension(extension.getName());

        // TODO: Have a nice factory way to load this...
        // Some preprocessing for certain builds
        if (pageBuilder.getClass().equals(QuteDataPageBuilder.class)) {
            return buildQutePage(pageBuilder, buildTimeData);
        }

        return pageBuilder.build();
    }

    private Page buildQutePage(PageBuilder pageBuilder, Map<String, BuildTimeData> buildTimeData) {
        try {
            QuteDataPageBuilder quteDataPageBuilder = (QuteDataPageBuilder) pageBuilder;
            String templatePath = quteDataPageBuilder.getTemplatePath();
            ClassPathUtils.consumeAsPaths(templatePath, p -> {
                try {
                    String template = Files.readString(p);
                    Map<String, Object> contentMap = buildTimeData.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().getContent()));
                    String fragment = Qute.fmt(template, contentMap);
                    pageBuilder.metadata("htmlFragment", fragment);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return pageBuilder.build();
    }

    private Class toClass(Type type) {
        if (type.kind().equals(Type.Kind.PRIMITIVE)) {
            return JandexReflection.loadRawType(type);
        } else if (type.kind().equals(Type.Kind.VOID)) {
            throw new RuntimeException("Void method return detected, JsonRPC Method needs to return something.");
        } else {
            try {
                return tccl.loadClass(type.name().toString());
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private Map<String, CardPageBuildItem> getCardPagesMap(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<CardPageBuildItem> pages) {
        Map<String, CardPageBuildItem> m = new HashMap<>();
        for (CardPageBuildItem pageBuildItem : pages) {
            String name = pageBuildItem.getExtensionPathName(curateOutcomeBuildItem);
            m.put(name, pageBuildItem);
        }
        return m;
    }

    private Map<String, MenuPageBuildItem> getMenuPagesMap(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<MenuPageBuildItem> pages) {
        Map<String, MenuPageBuildItem> m = new HashMap<>();
        for (MenuPageBuildItem pageBuildItem : pages) {
            m.put(pageBuildItem.getExtensionPathName(curateOutcomeBuildItem), pageBuildItem);
        }
        return m;
    }

    private Map<String, List<FooterPageBuildItem>> getFooterPagesMap(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<FooterPageBuildItem> pages) {
        Map<String, List<FooterPageBuildItem>> m = new HashMap<>();
        for (FooterPageBuildItem pageBuildItem : pages) {
            String key = pageBuildItem.getExtensionPathName(curateOutcomeBuildItem);
            if (m.containsKey(key)) {
                m.get(key).add(pageBuildItem);
            } else {
                List<FooterPageBuildItem> fbi = new ArrayList<>();
                fbi.add(pageBuildItem);
                m.put(key, fbi);
            }
        }
        return m;
    }

    private Map<String, List<SettingPageBuildItem>> getSettingPagesMap(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<SettingPageBuildItem> pages) {
        Map<String, List<SettingPageBuildItem>> m = new HashMap<>();
        for (SettingPageBuildItem pageBuildItem : pages) {
            String key = pageBuildItem.getExtensionPathName(curateOutcomeBuildItem);
            if (m.containsKey(key)) {
                m.get(key).add(pageBuildItem);
            } else {
                List<SettingPageBuildItem> sbi = new ArrayList<>();
                sbi.add(pageBuildItem);
                m.put(key, sbi);
            }
        }
        return m;
    }

    private Map<String, List<UnlistedPageBuildItem>> getUnlistedPagesMap(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<UnlistedPageBuildItem> pages) {
        Map<String, List<UnlistedPageBuildItem>> m = new HashMap<>();
        for (UnlistedPageBuildItem pageBuildItem : pages) {
            String key = pageBuildItem.getExtensionPathName(curateOutcomeBuildItem);
            if (m.containsKey(key)) {
                m.get(key).add(pageBuildItem);
            } else {
                List<UnlistedPageBuildItem> ubi = new ArrayList<>();
                ubi.add(pageBuildItem);
                m.put(key, ubi);
            }
        }
        return m;
    }

    private String getExtensionNamespace(Map<String, Object> extensionMap) {
        final String artifactId;
        final String artifact = (String) extensionMap.get("artifact");
        if (artifact == null) {
            // trying quarkus 1.x format
            artifactId = (String) extensionMap.get("artifact-id");
            if (artifactId == null) {
                throw new RuntimeException(
                        "Failed to locate 'artifact' or 'group-id' and 'artifact-id' among metadata keys "
                                + extensionMap.keySet());
            }
        } else {
            final GACTV coords = GACTV.fromString(artifact);
            artifactId = coords.getArtifactId();
        }
        return artifactId;
    }

    // Sort extensions with Guide first and then alphabetical
    private final Comparator sortingComparator = new Comparator<Extension>() {
        @Override
        public int compare(Extension t, Extension t1) {
            if (t.getGuide() != null && t1.getGuide() != null) {
                return t.getName().compareTo(t1.getName());
            } else if (t.getGuide() == null) {
                return 1;
            } else {
                return -1;
            }
        }
    };
}
