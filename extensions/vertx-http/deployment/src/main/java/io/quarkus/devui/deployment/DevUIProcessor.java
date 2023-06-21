package io.quarkus.devui.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
import io.quarkus.deployment.IsDevelopment;
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
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.spi.DevUIContent;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.StaticContentBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.devui.spi.page.QuteDataPageBuilder;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.qute.Qute;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResourcesFilter;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleCORSFilter;
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

    private static final String DEVUI = "dev-ui";
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String DOUBLE_POINT = ":";
    private static final String DASH_DEPLOYMENT = "-deployment";
    private static final String SLASH_ALL = SLASH + "*";
    private static final String JSONRPC = "json-rpc-ws";

    private static final String CONSTRUCTOR = "<init>";

    private final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    private static final String JAR = "jar";
    private static final GACT UI_JAR = new GACT("io.quarkus", "quarkus-vertx-http-dev-ui-resources", null, JAR);
    private static final String YAML_FILE = "/META-INF/quarkus-extension.yaml";
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
    private static final String CODESTART = "codestart";
    private static final String LANGUAGES = "languages";

    private static final Logger log = Logger.getLogger(DevUIProcessor.class);

    @BuildStep(onlyIf = IsDevelopment.class)
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
            ShutdownContextBuildItem shutdownContext) throws IOException {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        if (devUIConfig.cors.enabled) {
            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .orderedRoute(DEVUI + SLASH_ALL, -1 * FilterBuildItem.CORS)
                    .handler(new DevConsoleCORSFilter())
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
                builder = builder.displayOnNotFoundPage("Dev UI (v2)");
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
                Files.write(tempFile, parsedContent.getBytes(StandardCharsets.UTF_8));

                urlAndPath.put(c.getFileName(), tempFile.toString());
            }
            Handler<RoutingContext> buildTimeStaticHandler = recorder.buildTimeStaticHandler(basepath, urlAndPath);

            routeProducer.produce(
                    nonApplicationRootPathBuildItem.routeBuilder().route(DEVUI + SLASH_ALL)
                            .handler(buildTimeStaticHandler)
                            .build());
        }

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
    }

    /**
     * This makes sure the JsonRPC Classes for both the internal Dev UI and extensions is available as a bean and on the index.
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexProducer,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonRpcRouter.class)
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
    @BuildStep(onlyIf = IsDevelopment.class)
    void findAllJsonRPCMethods(BuildProducer<JsonRPCMethodsBuildItem> jsonRPCMethodsProvider,
            BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer,
            LaunchModeBuildItem launchModeBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {

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

        if (!extensionMethodsMap.isEmpty()) {
            jsonRPCMethodsProvider.produce(new JsonRPCMethodsBuildItem(extensionMethodsMap));
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

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    void createJsonRpcRouter(DevUIRecorder recorder,
            BeanContainerBuildItem beanContainer,
            JsonRPCMethodsBuildItem jsonRPCMethodsBuildItem) {

        if (jsonRPCMethodsBuildItem != null) {
            Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap = jsonRPCMethodsBuildItem
                    .getExtensionMethodsMap();

            DevConsoleManager.setGlobal(DevUIRecorder.DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY,
                    JsonMapper.Factory.deploymentLinker().createLinkData(new DevUIDatabindCodec.Factory()));
            recorder.createJsonRpcRouter(beanContainer.getValue(), extensionMethodsMap);
        }
    }

    /**
     * This build all the pages for dev ui, based on the extension included
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    @SuppressWarnings("unchecked")
    void getAllExtensions(List<CardPageBuildItem> cardPageBuildItems,
            List<MenuPageBuildItem> menuPageBuildItems,
            List<FooterPageBuildItem> footerPageBuildItems,
            LaunchModeBuildItem launchModeBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ExtensionsBuildItem> extensionsProducer,
            BuildProducer<WebJarBuildItem> webJarBuildProducer,
            BuildProducer<DevUIWebJarBuildItem> devUIWebJarProducer) {

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

        // Now go through all extensions and check them for active components
        Map<String, CardPageBuildItem> cardPagesMap = getCardPagesMap(curateOutcomeBuildItem, cardPageBuildItems);
        Map<String, MenuPageBuildItem> menuPagesMap = getMenuPagesMap(curateOutcomeBuildItem, menuPageBuildItems);
        Map<String, FooterPageBuildItem> footerPagesMap = getFooterPagesMap(curateOutcomeBuildItem, footerPageBuildItems);
        try {
            final Yaml yaml = new Yaml();
            List<Extension> activeExtensions = new ArrayList<>();
            List<Extension> inactiveExtensions = new ArrayList<>();
            List<Extension> sectionMenuExtensions = new ArrayList<>();
            List<Extension> footerTabExtensions = new ArrayList<>();
            ClassPathUtils.consumeAsPaths(YAML_FILE, (Path p) -> {
                try {
                    Extension extension = new Extension();
                    final String extensionYaml;
                    try (Scanner scanner = new Scanner(Files.newBufferedReader(p, StandardCharsets.UTF_8))) {
                        scanner.useDelimiter("\\A");
                        extensionYaml = scanner.hasNext() ? scanner.next() : null;
                    }
                    if (extensionYaml == null) {
                        // This is a internal extension (like this one, Dev UI)
                        return;
                    }

                    final Map<String, Object> extensionMap = yaml.load(extensionYaml);

                    if (extensionMap.containsKey(NAME)) {
                        String namespace = getExtensionNamespace(extensionMap);
                        extension.setNamespace(namespace);
                        extension.setName((String) extensionMap.get(NAME));
                        extension.setDescription((String) extensionMap.getOrDefault(DESCRIPTION, null));
                        String artifactId = (String) extensionMap.getOrDefault(ARTIFACT, null);
                        extension.setArtifact(artifactId);

                        Map<String, Object> metaData = (Map<String, Object>) extensionMap.getOrDefault(METADATA, null);
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
                        extension.setExtensionDependencies((List<String>) metaData.getOrDefault(EXTENSION_DEPENDENCIES, null));
                        extension.setUnlisted(String.valueOf(metaData.getOrDefault(UNLISTED, false)));

                        if (metaData.containsKey(CAPABILITIES)) {
                            Map<String, Object> capabilities = (Map<String, Object>) metaData.get(CAPABILITIES);
                            extension.setProvidesCapabilities((List<String>) capabilities.getOrDefault(PROVIDES, null));
                        }

                        if (metaData.containsKey(CODESTART)) {
                            Map<String, Object> codestartMap = (Map<String, Object>) metaData.get(metaData);
                            if (codestartMap != null) {
                                Codestart codestart = new Codestart();
                                codestart.setName((String) codestartMap.getOrDefault(NAME, null));
                                codestart.setLanguages((List<String>) codestartMap.getOrDefault(LANGUAGES, null));
                                codestart.setArtifact((String) codestartMap.getOrDefault(ARTIFACT, null));
                                extension.setCodestart(codestart);
                            }
                        }

                        if (!cardPagesMap.containsKey(namespace)) { // Inactive
                            inactiveExtensions.add(extension);
                        } else { // Active
                            CardPageBuildItem cardPageBuildItem = cardPagesMap.get(namespace);

                            // Add all card links
                            List<PageBuilder> cardPageBuilders = cardPageBuildItem.getPages();

                            Map<String, Object> buildTimeData = cardPageBuildItem.getBuildTimeData();
                            for (PageBuilder pageBuilder : cardPageBuilders) {
                                Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                extension.addCardPage(page);
                            }

                            // See if there is a custom card component
                            cardPageBuildItem.getOptionalCard().ifPresent((card) -> {
                                card.setNamespace(extension.getNamespace());
                                extension.setCard(card);
                            });

                            // Also make sure the static resources for that static resource is available
                            produceResources(artifactId, webJarBuildProducer,
                                    devUIWebJarProducer);
                            activeExtensions.add(extension);
                        }

                        // Menus on the sections menu
                        if (menuPagesMap.containsKey(namespace)) {
                            MenuPageBuildItem menuPageBuildItem = menuPagesMap.get(namespace);
                            List<PageBuilder> menuPageBuilders = menuPageBuildItem.getPages();

                            Map<String, Object> buildTimeData = menuPageBuildItem.getBuildTimeData();
                            for (PageBuilder pageBuilder : menuPageBuilders) {
                                Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                extension.addMenuPage(page);
                            }
                            // Also make sure the static resources for that static resource is available
                            produceResources(artifactId, webJarBuildProducer,
                                    devUIWebJarProducer);
                            sectionMenuExtensions.add(extension);
                        }

                        // Tabs in the footer
                        if (footerPagesMap.containsKey(namespace)) {
                            FooterPageBuildItem footerPageBuildItem = footerPagesMap.get(namespace);
                            List<PageBuilder> footerPageBuilders = footerPageBuildItem.getPages();

                            Map<String, Object> buildTimeData = footerPageBuildItem.getBuildTimeData();
                            for (PageBuilder pageBuilder : footerPageBuilders) {
                                Page page = buildFinalPage(pageBuilder, extension, buildTimeData);
                                extension.addFooterPage(page);
                            }
                            // Also make sure the static resources for that static resource is available
                            produceResources(artifactId, webJarBuildProducer,
                                    devUIWebJarProducer);
                            footerTabExtensions.add(extension);
                        }

                    }

                    Collections.sort(activeExtensions, sortingComparator);
                    Collections.sort(inactiveExtensions, sortingComparator);
                } catch (IOException | RuntimeException e) {
                    // don't abort, just log, to prevent a single extension from breaking entire dev ui
                    log.error("Failed to process extension descriptor " + p.toUri(), e);
                }
            });
            extensionsProducer.produce(
                    new ExtensionsBuildItem(activeExtensions, inactiveExtensions, sectionMenuExtensions, footerTabExtensions));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

    private void produceResources(String artifactId,
            BuildProducer<WebJarBuildItem> webJarBuildProducer,
            BuildProducer<DevUIWebJarBuildItem> devUIWebJarProducer) {

        GACT gact = getGACT(artifactId);
        String namespace = getNamespace(gact);
        if (namespace.isEmpty()) {
            namespace = "devui";
        }
        String buildTimeDataImport = namespace + "-data";

        webJarBuildProducer.produce(WebJarBuildItem.builder()
                .artifactKey(gact)
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

        devUIWebJarProducer.produce(
                new DevUIWebJarBuildItem(gact,
                        DEVUI));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
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

    private String getNamespace(GACT artifactKey) {
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

    private GACT getGACT(String artifactKey) {
        String[] split = artifactKey.split(DOUBLE_POINT);
        return new GACT(split[0], split[1] + DASH_DEPLOYMENT, null, JAR);
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

    private Map<String, FooterPageBuildItem> getFooterPagesMap(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<FooterPageBuildItem> pages) {
        Map<String, FooterPageBuildItem> m = new HashMap<>();
        for (FooterPageBuildItem pageBuildItem : pages) {
            m.put(pageBuildItem.getExtensionPathName(curateOutcomeBuildItem), pageBuildItem);
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
