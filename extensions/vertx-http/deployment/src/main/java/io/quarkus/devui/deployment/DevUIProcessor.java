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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import io.quarkus.devui.runtime.DevUIRecorder;
import io.quarkus.devui.runtime.VertxRouteInfoService;
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.spi.DevUIContent;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.buildtime.FooterLogBuildItem;
import io.quarkus.devui.spi.buildtime.StaticContentBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.LibraryLink;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.devui.spi.page.QuteDataPageBuilder;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.qute.Qute;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResourcesFilter;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
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
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String SLASH_ALL = SLASH + "*";
    private static final String JSONRPC = "json-rpc-ws";

    private static final String CONSTRUCTOR = "<init>";

    private final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    private static final String JAR = "jar";
    private static final GACT UI_JAR = new GACT("io.quarkus", "quarkus-vertx-http-dev-ui-resources", null, JAR);
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
                .orderedRoute(DEVUI + SLASH_ALL, -2 * FilterBuildItem.CORS)
                .handler(recorder.createLocalHostOnlyFilter(devUIConfig.hosts().orElse(null)))
                .build());

        if (devUIConfig.cors().enabled()) {
            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .orderedRoute(DEVUI + SLASH_ALL, -1 * FilterBuildItem.CORS)
                    .handler(recorder.createDevUICorsFilter(devUIConfig.hosts().orElse(null)))
                    .build());
        }

        // Websocket for JsonRPC comms
        routeProducer.produce(
                nonApplicationRootPathBuildItem
                        .routeBuilder().route(DEVUI + SLASH + JSONRPC)
                        .handler(recorder.communicationHandler())
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

            List<DevUIContent> content = staticContentBuildItem.getContent();
            for (DevUIContent c : content) {
                String parsedContent = Qute.fmt(new String(c.getTemplate()), c.getData());
                Path tempFile = devUiBasePath
                        .resolve(c.getFileName());
                Files.writeString(tempFile, parsedContent);

                urlAndPath.put(c.getFileName(), tempFile.toString());
            }
            Handler<RoutingContext> buildTimeStaticHandler = recorder.buildTimeStaticHandler(basepath, urlAndPath);

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
                    .orderedRoute("/", Integer.MAX_VALUE)
                    .handler(recorder.redirect(contextRoot, "welcome"))
                    .build());
        }
    }

    private boolean hasOwnIndexHtml() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> jarsWithIndexHtml = tccl.getResources("META-INF/resources/index.html");
            return jarsWithIndexHtml.hasMoreElements();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * This makes sure the JsonRPC Classes for both the internal Dev UI and extensions is available as a bean and on the index.
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexProducer,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonRpcRouter.class)
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

        Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap = new HashMap<>(); // All methods so that we can build the reflection

        List<String> requestResponseMethods = new ArrayList<>(); // All requestResponse methods for validation on the client side
        List<String> subscriptionMethods = new ArrayList<>(); // All subscription methods for validation on the client side

        // Let's use the Jandex index to find all methods
        for (JsonRPCProvidersBuildItem jsonRPCProvidersBuildItem : jsonRPCProvidersBuildItems) {

            Class clazz = jsonRPCProvidersBuildItem.getJsonRPCMethodProviderClass();
            String extension = jsonRPCProvidersBuildItem.getExtensionPathName(curateOutcomeBuildItem);
            Map<JsonRpcMethodName, JsonRpcMethod> jsonRpcMethods = new HashMap<>();
            if (extensionMethodsMap.containsKey(extension)) {
                jsonRpcMethods = extensionMethodsMap.get(extension);
            }

            ClassInfo classInfo = index.getClassByName(DotName.createSimple(clazz.getName()));

            List<MethodInfo> methods = classInfo.methods();

            for (MethodInfo method : methods) {
                if (!method.name().equals(CONSTRUCTOR)) { // Ignore constructor
                    if (Modifier.isPublic(method.flags())) { // Only allow public methods
                        if (method.returnType().kind() != Type.Kind.VOID) { // Only allow method with response

                            // Create list of available methods for the Javascript side.
                            if (method.returnType().name().equals(DotName.createSimple(Multi.class.getName()))) {
                                subscriptionMethods.add(extension + DOT + method.name());
                            } else {
                                requestResponseMethods.add(extension + DOT + method.name());
                            }

                            // Also create the map to pass to the runtime for the relection calls
                            JsonRpcMethodName jsonRpcMethodName = new JsonRpcMethodName(method.name());
                            if (method.parametersCount() > 0) {
                                Map<String, Class> params = new LinkedHashMap<>(); // Keep the order
                                for (int i = 0; i < method.parametersCount(); i++) {
                                    Type parameterType = method.parameterType(i);
                                    Class parameterClass = toClass(parameterType);
                                    String parameterName = method.parameterName(i);
                                    params.put(parameterName, parameterClass);
                                }
                                JsonRpcMethod jsonRpcMethod = new JsonRpcMethod(clazz, method.name(), params);
                                jsonRpcMethod.setExplicitlyBlocking(method.hasAnnotation(Blocking.class));
                                jsonRpcMethod
                                        .setExplicitlyNonBlocking(method.hasAnnotation(NonBlocking.class));
                                jsonRpcMethods.put(jsonRpcMethodName, jsonRpcMethod);
                            } else {
                                JsonRpcMethod jsonRpcMethod = new JsonRpcMethod(clazz, method.name(), null);
                                jsonRpcMethod.setExplicitlyBlocking(method.hasAnnotation(Blocking.class));
                                jsonRpcMethod
                                        .setExplicitlyNonBlocking(method.hasAnnotation(NonBlocking.class));
                                jsonRpcMethods.put(jsonRpcMethodName, jsonRpcMethod);
                            }
                        }
                    }
                }
            }

            if (!jsonRpcMethods.isEmpty()) {
                extensionMethodsMap.put(extension, jsonRpcMethods);
            }
        }

        if (deploymentMethodBuildItem.hasMethods()) {
            requestResponseMethods.addAll(deploymentMethodBuildItem.getMethods());
        }

        if (deploymentMethodBuildItem.hasSubscriptions()) {
            subscriptionMethods.addAll(deploymentMethodBuildItem.getSubscriptions());
        }

        if (!extensionMethodsMap.isEmpty()) {
            jsonRPCMethodsProvider.produce(new JsonRPCRuntimeMethodsBuildItem(extensionMethodsMap));
        }

        BuildTimeConstBuildItem methodInfo = new BuildTimeConstBuildItem("devui-jsonrpc");

        if (!subscriptionMethods.isEmpty()) {
            methodInfo.addBuildTimeData("jsonRPCSubscriptions", subscriptionMethods);
        }
        if (!requestResponseMethods.isEmpty()) {
            methodInfo.addBuildTimeData("jsonRPCMethods", requestResponseMethods);
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
            Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap = jsonRPCMethodsBuildItem
                    .getExtensionMethodsMap();

            DevConsoleManager.setGlobal(DevUIRecorder.DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY,
                    JsonMapper.Factory.deploymentLinker().createLinkData(new DevUIDatabindCodec.Factory()));
            recorder.createJsonRpcRouter(beanContainer.getValue(), extensionMethodsMap, deploymentMethodBuildItem.getMethods(),
                    deploymentMethodBuildItem.getSubscriptions(), deploymentMethodBuildItem.getRecordedValues());
        }
    }

    /**
     * This build all the pages for dev ui, based on the extension included
     */
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
                devServiceLogActions.addSubscription(name + "Log", footerLogBuildItem.getRuntimePublisher());
            } else {
                devServiceLogActions.addSubscription(name + "Log", ignored -> {
                    try {
                        return footerLogBuildItem.getPublisher();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            devServiceLogs.add(devServiceLogActions);

            // Create the Footer in the Dev UI
            WebComponentPageBuilder log = Page.webComponentPageBuilder().internal()
                    .namespace(FOOTER_LOG_NAMESPACE)
                    .icon("font-awesome-regular:file-lines")
                    .title(capitalizeFirstLetter(footerLogBuildItem.getName()))
                    .metadata("jsonRpcMethodName", footerLogBuildItem.getName() + "Log")
                    .componentLink("qwc-footer-log.js");

            FooterPageBuildItem footerPageBuildItem = new FooterPageBuildItem(FOOTER_LOG_NAMESPACE, log);
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
            LaunchModeBuildItem launchModeBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ExtensionsBuildItem> extensionsProducer,
            BuildProducer<WebJarBuildItem> webJarBuildProducer,
            BuildProducer<DevUIWebJarBuildItem> devUIWebJarProducer,
            Capabilities capabilities) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            // produce extension build item as cascade of build steps rely on it
            var emptyExtensionBuildItem = new ExtensionsBuildItem(List.of(), List.of(), List.of(), List.of());
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

        final Yaml yaml = new Yaml();
        List<Extension> activeExtensions = new ArrayList<>();
        List<Extension> inactiveExtensions = new ArrayList<>();
        List<Extension> sectionMenuExtensions = new ArrayList<>();
        List<Extension> footerTabExtensions = new ArrayList<>();

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

                                    Map<String, Object> buildTimeData = cardPageBuildItem.getBuildTimeData();
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

                                    Map<String, Object> buildTimeData = menuPageBuildItem.getBuildTimeData();
                                    for (PageBuilder pageBuilder : menuPageBuilders) {
                                        Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                        if (!page.isAssistantPage() || assistantIsAvailable) {
                                            extension.addMenuPage(page);
                                        }
                                    }
                                    // Also make sure the static resources for that static resource is available
                                    produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                    sectionMenuExtensions.add(extension);
                                }

                                // Tabs in the footer
                                if (footerPagesMap.containsKey(namespace)) {

                                    List<FooterPageBuildItem> fbis = footerPagesMap.get(namespace);
                                    for (FooterPageBuildItem footerPageBuildItem : fbis) {
                                        List<PageBuilder> footerPageBuilders = footerPageBuildItem.getPages();

                                        Map<String, Object> buildTimeData = footerPageBuildItem.getBuildTimeData();
                                        for (PageBuilder pageBuilder : footerPageBuilders) {
                                            Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                            if (!page.isAssistantPage() || assistantIsAvailable) {
                                                extension.addFooterPage(page);
                                            }
                                        }
                                        // Also make sure the static resources for that static resource is available
                                        produceResources(runtimeExt, webJarBuildProducer, devUIWebJarProducer);
                                        footerTabExtensions.add(extension);
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
                    if (footerPageBuildItem.isInternal()) {
                        Extension deploymentOnlyExtension = new Extension();
                        deploymentOnlyExtension.setName(footer.getKey());
                        deploymentOnlyExtension.setNamespace(FOOTER_LOG_NAMESPACE);

                        List<PageBuilder> footerPageBuilders = footerPageBuildItem.getPages();

                        for (PageBuilder pageBuilder : footerPageBuilders) {
                            pageBuilder.namespace(deploymentOnlyExtension.getNamespace());
                            pageBuilder.extension(deploymentOnlyExtension.getName());
                            pageBuilder.internal();
                            Page page = pageBuilder.build();
                            deploymentOnlyExtension.addFooterPage(page);
                        }

                        footerTabExtensions.add(deploymentOnlyExtension);
                    }
                }
            }
        }

        extensionsProducer.produce(
                new ExtensionsBuildItem(activeExtensions, inactiveExtensions, sectionMenuExtensions, footerTabExtensions));
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

        if (cardPageBuildItem != null) {
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
        String namespace = artifactKey.getGroupId() + "." + artifactKey.getArtifactId();

        if (namespace.equals("io.quarkus.quarkus-vertx-http-dev-ui-resources")) {
            // Internal
            namespace = "";
        } else if (namespace.endsWith("-deployment")) {
            int end = namespace.lastIndexOf("-");
            namespace = namespace.substring(0, end);
        }
        return namespace;
    }

    private Page buildFinalPage(PageBuilder pageBuilder, Extension extension, Map<String, Object> buildTimeData) {
        pageBuilder.namespace(extension.getNamespace());
        pageBuilder.extension(extension.getName());

        // TODO: Have a nice factory way to load this...
        // Some preprocessing for certain builds
        if (pageBuilder.getClass().equals(QuteDataPageBuilder.class)) {
            return buildQutePage(pageBuilder, extension, buildTimeData);
        }

        return pageBuilder.build();
    }

    private Page buildQutePage(PageBuilder pageBuilder, Extension extension, Map<String, Object> buildTimeData) {
        try {
            QuteDataPageBuilder quteDataPageBuilder = (QuteDataPageBuilder) pageBuilder;
            String templatePath = quteDataPageBuilder.getTemplatePath();
            ClassPathUtils.consumeAsPaths(templatePath, p -> {
                try {
                    String template = Files.readString(p);
                    String fragment = Qute.fmt(template, buildTimeData);
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

    private String getExtensionNamespace(Map<String, Object> extensionMap) {
        final String groupId;
        final String artifactId;
        final String artifact = (String) extensionMap.get("artifact");
        if (artifact == null) {
            // trying quarkus 1.x format
            groupId = (String) extensionMap.get("group-id");
            artifactId = (String) extensionMap.get("artifact-id");
            if (artifactId == null || groupId == null) {
                throw new RuntimeException(
                        "Failed to locate 'artifact' or 'group-id' and 'artifact-id' among metadata keys "
                                + extensionMap.keySet());
            }
        } else {
            final GACTV coords = GACTV.fromString(artifact);
            groupId = coords.getGroupId();
            artifactId = coords.getArtifactId();
        }
        return groupId + "." + artifactId;
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
