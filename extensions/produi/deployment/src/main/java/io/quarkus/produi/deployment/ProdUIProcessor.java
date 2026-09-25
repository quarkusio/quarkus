package io.quarkus.produi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.mvnpm.esbuild.Bundler;
import io.mvnpm.esbuild.model.BundleOptions;
import io.mvnpm.esbuild.model.BundleOptionsBuilder;
import io.mvnpm.esbuild.model.BundleResult;
import io.mvnpm.esbuild.model.EsBuildConfig;
import io.mvnpm.esbuild.model.WebDependency;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.util.ExtensionMetadataUtil;
import io.quarkus.builder.Version;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeData;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.produi.runtime.ProdUIBuildTimeConfig;
import io.quarkus.produi.runtime.ProdUIPageVisibility;
import io.quarkus.produi.runtime.ProdUIRecorder;
import io.quarkus.produi.runtime.ProdUISecurity;
import io.quarkus.produi.runtime.ProdUISelfFilter;
import io.quarkus.produi.runtime.advisor.AdvisorProdUIService;
import io.quarkus.produi.runtime.config.ConfigProdUIService;
import io.quarkus.produi.runtime.diagnostics.DiagnosticsProdUIService;
import io.quarkus.produi.runtime.endpoints.EndpointsProdUIService;
import io.quarkus.produi.runtime.logging.LoggingProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class ProdUIProcessor {

    private static final Logger log = Logger.getLogger(ProdUIProcessor.class);
    private static final String FEATURE = "produi";
    private static final String CONSTRUCTOR = "<init>";
    private static final String UNDERSCORE = "_";
    private static final String SLASH = "/";
    private static final String META_INF_RESOURCES = "META-INF/resources/";

    @BuildStep(onlyIf = IsProduction.class)
    void feature(ProdUIBuildTimeConfig config,
            BuildProducer<FeatureBuildItem> featureProducer) {
        if (config.enabled()) {
            featureProducer.produce(new FeatureBuildItem(FEATURE));
        }
    }

    @BuildStep(onlyIf = IsProduction.class)
    void protectProdUI(ProdUIBuildTimeConfig config,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigProducer) {

        if (!config.enabled() || config.rolesAllowed().isEmpty() || config.rolesAllowed().get().isEmpty()) {
            return;
        }

        // Prod UI is served on the management interface when it is enabled, otherwise on the main interface.
        // Restrict access on whichever interface actually carries the routes - including the json-rpc-ws data plane.
        boolean onManagement = managementBuildTimeConfig.enabled();
        String authConfigPrefix = onManagement ? "quarkus.management.auth" : "quarkus.http.auth";
        String path = onManagement
                ? nonApplicationRootPathBuildItem.resolveManagementPath(config.path(), managementBuildTimeConfig,
                        launchModeBuildItem, false)
                : nonApplicationRootPathBuildItem.resolvePath(config.path());

        Map<String, String> defaults = ProdUISecurity.authConfigDefaults(authConfigPrefix, path,
                config.rolesAllowed().get());
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            runTimeConfigProducer.produce(new RunTimeConfigurationDefaultBuildItem(entry.getKey(), entry.getValue()));
        }
    }

    @BuildStep(onlyIf = IsProduction.class)
    void registerReflectionForNative(ProdUIBuildTimeConfig config,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {

        if (!config.enabled()) {
            return;
        }

        // The JSON-RPC router invokes provider methods reflectively
        // (providerInstance.getClass().getMethod(...).invoke(target, args)). Dev UI only ever runs in JVM dev mode, so
        // those methods were never registered for reflection. Prod UI runs in production - including native - so register
        // the methods of every provider that actually exposes a PROD_UI method, otherwise the call fails in native with a
        // missing-method / not-registered error.
        for (JsonRPCProvidersBuildItem provider : jsonRPCProvidersBuildItems) {
            Class<?> clazz = provider.getJsonRPCMethodProviderClass();
            if (hasProdUIMethod(clazz)) {
                reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(clazz)
                        .methods()
                        .build());
            }
        }
    }

    @BuildStep
    void registerBuiltInJsonRPCProviders(ProdUIBuildTimeConfig config,
            BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProviders) {
        jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", ConfigProdUIService.class));
        jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", EndpointsProdUIService.class));
        jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", LoggingProdUIService.class));
        jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", AdvisorProdUIService.class));

        // The gated thread dump is the only non-read-only-by-default action; only expose its method when the operator
        // has explicitly opted in, so a default Prod UI stays strictly read-only.
        if (config.diagnostics().threadDump()) {
            jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", DiagnosticsProdUIService.class));
        }
    }

    @BuildStep(onlyIf = IsProduction.class)
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            ProdUIBuildTimeConfig config,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {

        if (!config.enabled()) {
            return;
        }

        // NOTE: the JSON-RPC provider classes are intentionally NOT contributed as
        // AdditionalIndexedClassesBuildItem here. Doing so feeds the combined index,
        // which several JsonRPCProvidersBuildItem producers (e.g. resteasy-reactive)
        // transitively depend on, forming a build-step cycle. Instead setupProdUI
        // indexes these classes into a small supplemental index for method discovery.
        for (JsonRPCProvidersBuildItem provider : jsonRPCProvidersBuildItems) {
            Class<?> clazz = provider.getJsonRPCMethodProviderClass();

            // Only register providers that actually expose a PROD_UI method. Many
            // extensions contribute a JsonRPCProvidersBuildItem for their Dev UI
            // service unconditionally (in all launch modes); those Dev-UI-only
            // services often depend on dev-only beans and must not be turned into
            // beans in production.
            if (!hasProdUIMethod(clazz)) {
                continue;
            }

            DotName scope = provider.getDefaultBeanScope() == null
                    ? BuiltinScope.APPLICATION.getName()
                    : provider.getDefaultBeanScope();

            additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(clazz)
                    .setDefaultScope(scope)
                    .setUnremovable()
                    .build());
        }

        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonRpcRouter.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable()
                .build());
    }

    @BuildStep(onlyIf = IsProduction.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupProdUI(ProdUIRecorder recorder,
            ProdUIBuildTimeConfig config,
            BeanContainerBuildItem beanContainer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems,
            List<ProdUIPageBuildItem> pages,
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) {

        if (!config.enabled()) {
            return;
        }

        IndexView index = combinedIndexBuildItem.getIndex();
        String prodUIPath = config.path();
        String nonAppRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath();
        String contextRoot = nonAppRoot + prodUIPath + SLASH;
        String resourcePrefix = prodUIPath + SLASH;

        // 1. Discover PROD_UI JsonRPC methods
        Map<String, JsonRpcMethod> runtimeMethods = new HashMap<>();
        Map<String, JsonRpcMethod> runtimeSubscriptions = new HashMap<>();
        List<String> methodNames = new ArrayList<>();
        List<String> subscriptionNames = new ArrayList<>();

        // Page visibility (quarkus.prod-ui.pages.<id>.enabled). Used both to gate the nav cards and to drop the
        // json-rpc methods of a fully-hidden extension namespace from the data plane.
        Map<String, Boolean> enabledByPageId = new HashMap<>();
        config.pages().forEach((id, pageConfig) -> enabledByPageId.put(id, pageConfig.enabled()));

        // The Diagnostics page (gated thread dump) is only shown when the action is enabled; Prod UI is read-only by
        // default. When enabled, operators can still hide it via quarkus.prod-ui.pages.diagnostics.enabled=false.
        if (!config.diagnostics().threadDump()) {
            enabledByPageId.put("diagnostics", false);
        }

        // The JSON-RPC provider classes are not guaranteed to be in the combined
        // index (see registerBeans), so discover their methods against a composite
        // of the combined index plus a supplemental index of the provider classes.
        IndexView discoveryIndex = withProviderClasses(index, jsonRPCProvidersBuildItems);
        discoverProdUIMethods(discoveryIndex, curateOutcomeBuildItem, jsonRPCProvidersBuildItems, enabledByPageId,
                runtimeMethods, runtimeSubscriptions, methodNames, subscriptionNames);

        // 2. Initialize the JsonRPC router and register endpoints
        recorder.initializeJsonRpcRouter(beanContainer.getValue(), runtimeMethods, runtimeSubscriptions);

        List<String> endpointPaths = collectEndpoints(index);
        recorder.registerEndpoints(beanContainer.getValue(), endpointPaths);

        // 3. Generate dynamic data files
        String pagesDataJs = generatePagesDataJs(pages, curateOutcomeBuildItem, enabledByPageId);
        embedResource(resourcePrefix + "produi-pages-data.js", pagesDataJs.getBytes(StandardCharsets.UTF_8),
                generatedResourceProducer, nativeImageResourceProducer);

        String jsonRpcDataJs = "export const jsonRPCMethods = " + toJsonArray(methodNames) + ";\n"
                + "export const jsonRPCSubscriptions = " + toJsonArray(subscriptionNames) + ";\n";
        embedResource(resourcePrefix + "produi-jsonrpc-data.js", jsonRpcDataJs.getBytes(StandardCharsets.UTF_8),
                generatedResourceProducer, nativeImageResourceProducer);

        String uiContextJs = "export const isProdUI = true;\nexport const isDevUI = false;\n";
        embedResource(resourcePrefix + "ui-context.js", uiContextJs.getBytes(StandardCharsets.UTF_8),
                generatedResourceProducer, nativeImageResourceProducer);

        String appInfoJs = generateAppInfoJs();
        embedResource(resourcePrefix + "produi-app-info.js", appInfoJs.getBytes(StandardCharsets.UTF_8),
                generatedResourceProducer, nativeImageResourceProducer);

        // Generate dependencies data (build-time data from CurateOutcomeBuildItem)
        String depsDataJs = generateDependenciesDataJs(curateOutcomeBuildItem, config.excludeSelf());
        embedResource(resourcePrefix + "produi-dependencies-data.js", depsDataJs.getBytes(StandardCharsets.UTF_8),
                generatedResourceProducer, nativeImageResourceProducer);

        // Generate build-time data JS files for each extension
        generateAndEmbedBuildTimeDataFiles(pages, curateOutcomeBuildItem, resourcePrefix,
                generatedResourceProducer, nativeImageResourceProducer);

        // 4. Bundle the shell components and extension pages with esbuild
        bundleAndEmbed(curateOutcomeBuildItem, pages, resourcePrefix, contextRoot,
                generatedResourceProducer, nativeImageResourceProducer);

        // 5. Extract and embed Lumo CSS
        extractLumoCss(curateOutcomeBuildItem, resourcePrefix, generatedResourceProducer, nativeImageResourceProducer);

        // 6. Generate and embed index.html
        String indexHtml = generateIndexHtml(contextRoot, resourcePrefix);
        embedResource(resourcePrefix + "index.html", indexHtml.getBytes(StandardCharsets.UTF_8),
                generatedResourceProducer, nativeImageResourceProducer);

        // 6. Register routes on management interface
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .route(prodUIPath + "/json-rpc-ws")
                .handler(recorder.prodUIWebSocketHandler())
                .build());

        String prodUIWebRoot = META_INF_RESOURCES + prodUIPath;
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .route(prodUIPath + "/*")
                .handler(recorder.classpathStaticHandler(prodUIWebRoot))
                .build());

        // SPA fallback: serve index.html for routes without a file extension
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .route(prodUIPath + "/*")
                .handler(recorder.spaFallbackHandler(nonAppRoot + prodUIPath + "/index.html"))
                .build());
    }

    private void bundleAndEmbed(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<ProdUIPageBuildItem> pages,
            String resourcePrefix,
            String contextRoot,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) {

        try {
            Path workDir = Files.createTempDirectory("produi-bundle");
            Path sourceDir = workDir.resolve("src");
            Files.createDirectories(sourceDir);

            // Copy shell component sources to the work directory
            copyShellSources(sourceDir);

            // Copy the Font Awesome iconset (imported by produi-app.js) so esbuild can bundle it
            copyIconSet(sourceDir);

            // Copy extension page components to the work directory
            copyExtensionPages(pages, curateOutcomeBuildItem, sourceDir);

            // Resolve mvnpm web dependencies from the application model
            List<WebDependency> webDeps = resolveWebDependencies(curateOutcomeBuildItem);

            // Configure esbuild
            // - Alias bare specifiers to Prod UI shims so Dev UI components work
            // - Mark dynamic data files as external (generated at build time, served separately)
            Map<String, String> aliases = new HashMap<>();
            aliases.put("jsonrpc", "./shims/jsonrpc.js");
            aliases.put("localization", "./shims/localization.js");
            aliases.put("ui-context", "./controller/ui-context.js");
            aliases.put("qwc-hot-reload-element", "./shims/qwc-hot-reload-element.js");
            aliases.put("storage-controller", "./shims/storage-controller.js");
            aliases.put("pui-echart-bar", "./pui/echarts/pui-echart-bar.js");
            aliases.put("pui-echart-gauge", "./pui/echarts/pui-echart-gauge.js");
            aliases.put("pui-echart-graph", "./pui/echarts/pui-echart-graph.js");

            EsBuildConfig esConfig = EsBuildConfig.builder()
                    .withDefault()
                    .fixedEntryNames()
                    .publicPath(contextRoot + "bundle/")
                    .alias(aliases)
                    .addExternal("../produi-pages-data.js")
                    .addExternal("../produi-jsonrpc-data.js")
                    .addExternal("../ui-context.js")
                    .addExternal("../produi-app-info.js")
                    .addExternal("../produi-dependencies-data.js")
                    .addExternal("*-data.js")
                    .addExternal("build-time-data")
                    .build();

            BundleOptionsBuilder optionsBuilder = BundleOptions.builder()
                    .withWorkDir(sourceDir)
                    .withNodeModulesDir(sourceDir.resolve("node_modules"))
                    .withDependencies(webDeps)
                    .addEntryPoint(sourceDir, "produi-app.js")
                    .withEsConfig(esConfig);

            // Add extension page components as additional entry points
            for (ProdUIPageBuildItem page : pages) {
                for (var pageBuilder : page.getPages()) {
                    var p = pageBuilder.build();
                    String componentLink = p.getComponentLink();
                    if (componentLink != null && Files.exists(sourceDir.resolve(componentLink))) {
                        optionsBuilder.addEntryPoint(sourceDir, componentLink);
                    }
                }
            }

            BundleResult result = Bundler.bundle(optionsBuilder.build(), true);

            // Embed all bundled output files (recursively)
            Path dist = result.dist();
            if (Files.exists(dist)) {
                embedDirectory(dist, dist, resourcePrefix + "bundle/",
                        generatedResourceProducer, nativeImageResourceProducer);
            }

            // Clean up
            deleteDirectory(workDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to bundle Prod UI frontend", e);
        }
    }

    private void embedDirectory(Path baseDir, Path currentDir, String prefix,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    embedDirectory(baseDir, entry, prefix, generatedResourceProducer, nativeImageResourceProducer);
                } else if (Files.isRegularFile(entry)) {
                    String relativePath = baseDir.relativize(entry).toString();
                    byte[] content = Files.readAllBytes(entry);
                    embedResource(prefix + relativePath, content,
                            generatedResourceProducer, nativeImageResourceProducer);
                }
            }
        }
    }

    private void copyShellSources(Path targetDir) throws IOException {
        String[] sources = {
                "prod-ui/produi-app.js",
                "prod-ui/controller/jsonrpc.js",
                "prod-ui/controller/ui-context.js",
                "prod-ui/pui/pui-app.js",
                "prod-ui/pui/pui-header.js",
                "prod-ui/pui/pui-extensions.js",
                "prod-ui/pui/pui-extension-link.js",
                "prod-ui/pui/pui-extension-text.js",
                "prod-ui/pui/pui-extension-dialog.js",
                "prod-ui/pui/pui-empty-state.js",
                "prod-ui/pui/pui-availability.js",
                "prod-ui/pui/pui-page-host.js",
                "prod-ui/pui/pui-configuration.js",
                "prod-ui/pui/pui-endpoints.js",
                "prod-ui/pui/pui-loggers.js",
                "prod-ui/pui/pui-dependencies.js",
                "prod-ui/pui/pui-advisor.js",
                "prod-ui/pui/pui-diagnostics.js",
                "prod-ui/pui/echarts/pui-echart.js",
                "prod-ui/pui/echarts/pui-echart-bar.js",
                "prod-ui/pui/echarts/pui-echart-gauge.js",
                "prod-ui/pui/echarts/pui-echart-graph.js",
                "prod-ui/shims/jsonrpc.js",
                "prod-ui/shims/localization.js",
                "prod-ui/shims/qwc-hot-reload-element.js",
                "prod-ui/shims/storage-controller.js"
        };

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String source : sources) {
            try (InputStream is = cl.getResourceAsStream(source)) {
                if (is != null) {
                    String relativePath = source.substring("prod-ui/".length());
                    Path target = targetDir.resolve(relativePath);
                    Files.createDirectories(target.getParent());
                    Files.write(target, is.readAllBytes());
                }
            }
        }
    }

    /**
     * Copy the Font Awesome iconset files from the {@code quarkus-devui-resources} jar (on the deployment
     * classpath) into the bundle work dir, under {@code icon/}, so the {@code import './icon/font-awesome.js'}
     * in {@code produi-app.js} resolves and esbuild bundles them. The aggregator {@code font-awesome.js} pulls
     * in the solid/regular/brands iconsets, each of which registers a {@code <vaadin-iconset>} on load. This is
     * what makes the {@code <vaadin-icon>} references on the extension cards actually paint.
     */
    private void copyIconSet(Path targetDir) throws IOException {
        String[] iconFiles = {
                "dev-ui/icon/font-awesome.js",
                "dev-ui/icon/font-awesome-solid.js",
                "dev-ui/icon/font-awesome-regular.js",
                "dev-ui/icon/font-awesome-brands.js"
        };
        Path iconDir = targetDir.resolve("icon");
        Files.createDirectories(iconDir);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String iconFile : iconFiles) {
            try (InputStream is = cl.getResourceAsStream(iconFile)) {
                if (is != null) {
                    String fileName = iconFile.substring("dev-ui/icon/".length());
                    Files.write(iconDir.resolve(fileName), is.readAllBytes());
                } else {
                    log.warnf("Prod UI: icon set resource not found on classpath: %s", iconFile);
                }
            }
        }
    }

    private void extractLumoCss(CurateOutcomeBuildItem curateOutcome,
            String resourcePrefix,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) {

        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
            if ("vaadin-webcomponents".equals(dep.getArtifactId())) {
                for (Path jarPath : dep.getResolvedPaths()) {
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.toFile())) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            java.util.jar.JarEntry entry = entries.nextElement();
                            if (entry.getName().endsWith("/dist/lumo.css")
                                    && entry.getName().contains("vaadin-lumo-styles")) {
                                try (InputStream is = jar.getInputStream(entry)) {
                                    embedResource(resourcePrefix + "lumo.css", is.readAllBytes(),
                                            generatedResourceProducer, nativeImageResourceProducer);
                                    return;
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.debug("Could not extract lumo.css", e);
                    }
                }
            }
        }
        log.warn("Lumo CSS not found in vaadin-webcomponents dependency");
    }

    private void copyExtensionPages(List<ProdUIPageBuildItem> pages,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            Path targetDir) throws IOException {
        // Scan all deployment dependency JARs for dev-ui/ resources
        // This copies all JS files (including sub-components like qwc-cache-keys.js)
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            if (!dep.getArtifactId().endsWith("-deployment")) {
                continue;
            }
            for (Path jarPath : dep.getResolvedPaths()) {
                if (!jarPath.toString().endsWith(".jar") || !Files.exists(jarPath)) {
                    continue;
                }
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.toFile())) {
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith("dev-ui/") && name.endsWith(".js") && !entry.isDirectory()) {
                            String relativeName = name.substring("dev-ui/".length());
                            try (InputStream is = jar.getInputStream(entry)) {
                                Path target = targetDir.resolve(relativeName);
                                Files.createDirectories(target.getParent());
                                Files.write(target, is.readAllBytes());
                            }
                        }
                    }
                } catch (IOException e) {
                    log.debugf(e, "Could not scan JAR for dev-ui resources: %s", jarPath);
                }
            }
        }
    }

    private List<WebDependency> resolveWebDependencies(CurateOutcomeBuildItem curateOutcome) {
        List<WebDependency> deps = new ArrayList<>();
        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
            String groupId = dep.getGroupId();
            if (groupId.startsWith("org.mvnpm")) {
                for (Path path : dep.getResolvedPaths()) {
                    deps.add(new WebDependency(
                            dep.getGroupId() + ":" + dep.getArtifactId(),
                            path,
                            WebDependency.WebDependencyType.MVNPM));
                }
            }
        }
        return deps;
    }

    private void embedResource(String path, byte[] content,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) {

        String classpathPath = META_INF_RESOURCES + path;
        generatedResourceProducer.produce(new GeneratedResourceBuildItem(classpathPath, content));
        nativeImageResourceProducer.produce(new NativeImageResourceBuildItem(classpathPath));
    }

    /**
     * Returns a view over the combined index augmented with a freshly built index
     * of the JSON-RPC provider classes. These classes are not fed into the combined
     * index (that would create a build-step cycle, see registerBeans), so they are
     * indexed here on demand purely for method discovery.
     */
    private IndexView withProviderClasses(IndexView combinedIndex,
            List<JsonRPCProvidersBuildItem> providers) {
        List<Class<?>> classes = new ArrayList<>();
        for (JsonRPCProvidersBuildItem provider : providers) {
            classes.add(provider.getJsonRPCMethodProviderClass());
        }
        try {
            Index providerIndex = Index.of(classes.toArray(new Class<?>[0]));
            return CompositeIndex.create(combinedIndex, providerIndex);
        } catch (IOException e) {
            // Fall back to the combined index; any provider class already indexed
            // there is still discovered, others are simply skipped.
            return combinedIndex;
        }
    }

    private void discoverProdUIMethods(IndexView index,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JsonRPCProvidersBuildItem> providers,
            Map<String, Boolean> enabledByPageId,
            Map<String, JsonRpcMethod> runtimeMethods,
            Map<String, JsonRpcMethod> runtimeSubscriptions,
            List<String> methodNames,
            List<String> subscriptionNames) {

        for (JsonRPCProvidersBuildItem provider : providers) {
            Class<?> clazz = provider.getJsonRPCMethodProviderClass();
            String extension = provider.getExtensionPathName(curateOutcomeBuildItem);

            // Drop the whole namespace from the data plane when its extension page is hidden, so the
            // <namespace>_* methods become unreachable over the websocket - not merely absent from the nav.
            // The built-in namespace is shared by several separately-gated pages and is never dropped here.
            if (!ProdUIPageVisibility.isNamespaceExposed(extension, enabledByPageId)) {
                continue;
            }

            ClassInfo classInfo = index.getClassByName(DotName.createSimple(clazz.getName()));
            if (classInfo == null) {
                continue;
            }

            for (MethodInfo method : classInfo.methods()) {
                if (method.name().equals(CONSTRUCTOR) || !Modifier.isPublic(method.flags())
                        || method.returnType().kind() == Type.Kind.VOID) {
                    continue;
                }

                EnumSet<Usage> usage = resolveUsage(method);
                if (!usage.contains(Usage.PROD_UI)) {
                    continue;
                }

                String methodName = extension + UNDERSCORE + method.name();

                Map<String, JsonRpcMethod.Parameter> parameters = new LinkedHashMap<>();
                for (int i = 0; i < method.parametersCount(); i++) {
                    boolean required = true;
                    Type parameterType = method.parameterType(i);
                    if (DotNames.OPTIONAL.equals(parameterType.name())) {
                        required = false;
                        parameterType = parameterType.asParameterizedType().arguments().get(0);
                    }
                    String description = null;
                    AnnotationInstance descAnnotation = method.parameters().get(i)
                            .annotation(DotName.createSimple(JsonRpcDescription.class));
                    if (descAnnotation != null && descAnnotation.value() != null) {
                        description = descAnnotation.value().asString();
                    }
                    Class<?> paramClass = toClass(parameterType);
                    parameters.put(method.parameterName(i),
                            new JsonRpcMethod.Parameter(paramClass, description, required));
                }

                JsonRpcMethod jsonRpcMethod = new JsonRpcMethod();
                jsonRpcMethod.setMethodName(method.name());
                jsonRpcMethod.setBean(clazz);
                jsonRpcMethod.setIsExplicitlyBlocking(method.hasAnnotation(Blocking.class));
                jsonRpcMethod.setIsExplicitlyNonBlocking(method.hasAnnotation(NonBlocking.class));
                if (!parameters.isEmpty()) {
                    for (Map.Entry<String, JsonRpcMethod.Parameter> p : parameters.entrySet()) {
                        jsonRpcMethod.addParameter(p.getKey(), p.getValue().getType(),
                                p.getValue().getDescription(), p.getValue().isRequired());
                    }
                }

                if (method.returnType().name().equals(DotName.createSimple(Multi.class.getName()))) {
                    runtimeSubscriptions.put(methodName, jsonRpcMethod);
                    subscriptionNames.add(methodName);
                } else {
                    runtimeMethods.put(methodName, jsonRpcMethod);
                    methodNames.add(methodName);
                }
            }
        }
    }

    private String generatePagesDataJs(List<ProdUIPageBuildItem> pages,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            Map<String, Boolean> enabledByPageId) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        // Built-in pages: { title, componentLink, page id }
        String[][] builtInPages = {
                { "Advisor", "pui-advisor", "advisor" },
                { "Configuration", "pui-configuration", "configuration" },
                { "Endpoints", "pui-endpoints", "endpoints" },
                { "Loggers", "pui-loggers", "loggers" },
                { "Dependencies", "pui-dependencies", "dependencies" },
                { "Diagnostics", "pui-diagnostics", "diagnostics" }
        };
        for (String[] bp : builtInPages) {
            // Operators can hide individual pages via quarkus.prod-ui.pages.<id>.enabled=false
            if (!ProdUIPageVisibility.isVisible(bp[2], enabledByPageId)) {
                continue;
            }
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("{\"namespace\":\"quarkus-produi\",");
            json.append("\"title\":\"").append(bp[0]).append("\",");
            json.append("\"pages\":[{\"title\":\"").append(bp[0]).append("\",");
            json.append("\"componentLink\":\"").append(bp[1]).append("\"}],");
            json.append("\"internal\":true}");
        }

        // Description/status come from each runtime extension's quarkus-extension.yaml metadata,
        // so every card gets real text without the extension having to opt in.
        Map<String, ExtensionMeta> metadata = collectExtensionMetadata(curateOutcomeBuildItem);

        // Extension-contributed pages: keyed by extension name (e.g. quarkus-cache)
        for (ProdUIPageBuildItem page : pages) {
            String namespace = page.getExtensionPathName(curateOutcomeBuildItem);
            if (!ProdUIPageVisibility.isVisible(namespace, enabledByPageId)) {
                continue;
            }
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("{\"namespace\":\"").append(escapeJson(namespace)).append("\",");
            json.append("\"title\":\"").append(escapeJson(formatTitle(namespace))).append("\"");
            ExtensionMeta meta = metadata.get(namespace);
            if (meta != null) {
                appendOptional(json, "description", meta.description());
                appendOptional(json, "status", meta.status());
                appendOptional(json, "logo", meta.logo());
                // Back-of-card details (all non-sensitive metadata from quarkus-extension.yaml).
                appendOptional(json, "guide", meta.guide());
                appendOptional(json, "categories", meta.categories());
                appendOptional(json, "keywords", meta.keywords());
                // Config prefixes drive the "View configuration" deep-link (filtered, masked).
                appendStringArray(json, "configPrefixes", meta.configPrefixes());
                // Library-version badges, resolved from the application model (mirrors Dev UI lib-ga).
                appendLibraries(json, meta.libraries());
            }
            json.append(",\"pages\":[");
            boolean firstPage = true;
            for (var pageBuilder : page.getPages()) {
                var p = pageBuilder.build();
                if (!firstPage) {
                    json.append(",");
                }
                firstPage = false;
                json.append("{\"title\":\"").append(escapeJson(p.getTitle())).append("\",");
                json.append("\"componentLink\":\"").append(escapeJson(p.getComponentLink())).append("\"");
                // External-link pages (Page.externalPageBuilder(...).url(...)) carry the target URL
                // in metadata. When present the shell opens it in a new tab instead of routing to a
                // component - used, e.g., to link to a Swagger UI that is served in production.
                if (p.getMetadata() != null) {
                    appendOptional(json, "externalUrl", p.getMetadata().get("externalUrl"));
                }
                // Optional per-link enrichment - icon, tooltip and a live/static badge label.
                // dynamicLabel/streamingLabel are JSON-RPC method names resolved by the frontend
                // over the same read-only channel; they are intended for counts/status only.
                appendOptional(json, "icon", p.getIcon());
                appendOptional(json, "tooltip", p.getTooltip());
                appendOptional(json, "staticLabel", p.getStaticLabel());
                appendOptional(json, "dynamicLabel", p.getDynamicLabel());
                appendOptional(json, "streamingLabel", p.getStreamingLabel());
                appendOptional(json, "streamingLabelParams", p.getStreamingLabelParams());
                json.append("}");
            }
            json.append("]");
            // Card texts - labelled rows (static or live) shown on the card body.
            if (page.hasCardTexts()) {
                json.append(",\"cardTexts\":[");
                boolean firstText = true;
                for (var text : page.getCardTexts()) {
                    if (!firstText) {
                        json.append(",");
                    }
                    firstText = false;
                    json.append("{");
                    boolean wrote = appendField(json, "title", text.getTitle(), true);
                    wrote = appendField(json, "icon", text.getIcon(), wrote) || wrote;
                    wrote = appendField(json, "staticText", text.getStaticText(), wrote) || wrote;
                    wrote = appendField(json, "dynamicText", text.getDynamicText(), wrote) || wrote;
                    wrote = appendField(json, "streamingText", text.getStreamingText(), wrote) || wrote;
                    appendField(json, "streamingTextParams", text.getStreamingTextParams(), wrote);
                    json.append("}");
                }
                json.append("]");
            }
            json.append("}");
        }
        json.append("]");
        return "export const pages = " + json + ";\n";
    }

    /**
     * Append a {@code ,"key":"value"} pair only when the value is non-null and non-empty.
     * Used for optional card/link fields so the emitted JSON stays compact.
     */
    static void appendOptional(StringBuilder json, String key, String value) {
        if (value != null && !value.isBlank()) {
            json.append(",\"").append(key).append("\":\"").append(escapeJson(value)).append("\"");
        }
    }

    /**
     * Append a {@code "key":"value"} pair inside an object literal, prefixing a comma when
     * something has already been written. Returns whether this call wrote anything.
     */
    static boolean appendField(StringBuilder json, String key, String value, boolean precededByField) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (precededByField) {
            json.append(",");
        }
        json.append("\"").append(key).append("\":\"").append(escapeJson(value)).append("\"");
        return true;
    }

    /**
     * Append a {@code ,"key":["a","b"]} array of strings only when the list is non-empty.
     */
    static void appendStringArray(StringBuilder json, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        json.append(",\"").append(key).append("\":[");
        boolean first = true;
        for (String v : values) {
            if (v == null) {
                continue;
            }
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(escapeJson(v)).append("\"");
        }
        json.append("]");
    }

    /** Append a {@code ,"libraries":[{name,version,url?}]} array only when there is at least one. */
    static void appendLibraries(StringBuilder json, List<Library> libraries) {
        if (libraries == null || libraries.isEmpty()) {
            return;
        }
        json.append(",\"libraries\":[");
        boolean first = true;
        for (Library lib : libraries) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("{\"name\":\"").append(escapeJson(lib.name()))
                    .append("\",\"version\":\"").append(escapeJson(lib.version())).append("\"");
            appendOptional(json, "url", lib.url());
            json.append("}");
        }
        json.append("]");
    }

    /**
     * Holds the human-friendly bits of an extension's {@code quarkus-extension.yaml} that make a card
     * less bare: its description, lifecycle status and logo (front of card) plus the guide URL,
     * categories, keywords, config prefixes and resolved library versions (back-of-card details). All
     * of this is non-sensitive build-time metadata - no runtime values, no secrets.
     */
    private record ExtensionMeta(String description, String status, String logo,
            String guide, String categories, String keywords,
            List<String> configPrefixes, List<Library> libraries) {
    }

    /** A resolved underlying-library link shown as a version badge (name = artifactId, mirrors Dev UI). */
    private record Library(String name, String version, String url) {
    }

    // lib-ga metadata format: groupId:artifactId[optionalUrl] (mirrors Dev UI's libGAPattern).
    private static final Pattern LIB_GA_PATTERN = Pattern.compile("([^:]+):([^\\[]+)(\\[(.*)\\])?");

    /**
     * Read {@code description} and {@code metadata.status} from every runtime extension's
     * {@code quarkus-extension.yaml} (mirrors how Dev UI populates its cards), keyed by
     * artifactId so it lines up with {@link ProdUIPageBuildItem#getExtensionPathName}.
     * Parsing failures degrade to no metadata for that extension - never a build failure.
     */
    private Map<String, ExtensionMeta> collectExtensionMetadata(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        Map<String, ExtensionMeta> result = new HashMap<>();
        var jsonMapper = JsonMapper.builder().build();
        var yamlMapper = YAMLMapper.builder().build();
        // groupId:artifactId -> version, used to resolve lib-ga library versions (mirrors Dev UI).
        Map<String, String> versionMap = new HashMap<>();
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            versionMap.putIfAbsent(dep.getGroupId() + ":" + dep.getArtifactId(), dep.getVersion());
        }
        for (ResolvedDependency ext : curateOutcomeBuildItem.getApplicationModel()
                .getDependencies(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
            ExtensionMetadataUtil.acceptExtensionMetadata(ext.getContentTree(), visit -> {
                if (visit == null) {
                    return;
                }
                try {
                    Path metadataFile = visit.getPath();
                    String content = Files.readString(metadataFile, StandardCharsets.UTF_8);
                    if (content == null || content.isBlank()) {
                        return; // internal extension (e.g. Prod UI itself) with no metadata
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = metadataFile.toString().endsWith(".json")
                            ? jsonMapper.readValue(content, Map.class)
                            : yamlMapper.readValue(content, Map.class);
                    String description = asString(map.get("description"));
                    String status = null;
                    String logo = null;
                    String guide = null;
                    String categories = null;
                    String keywords = null;
                    List<String> configPrefixes = null;
                    List<Library> libraries = null;
                    Object metaData = map.get("metadata");
                    if (metaData instanceof Map<?, ?> m) {
                        status = collectionToString(m.get("status"));
                        logo = asString(m.get("icon-url"));
                        guide = asString(m.get("guide"));
                        categories = collectionToString(m.get("categories"));
                        keywords = collectionToString(m.get("keywords"));
                        configPrefixes = toStringList(m.get("config"));
                        libraries = resolveLibraries(asString(m.get("lib-ga")), versionMap);
                    }
                    if (description != null || status != null || logo != null || guide != null
                            || categories != null || keywords != null
                            || (configPrefixes != null && !configPrefixes.isEmpty())
                            || (libraries != null && !libraries.isEmpty())) {
                        result.put(ext.getArtifactId(), new ExtensionMeta(description, status, logo,
                                guide, categories, keywords, configPrefixes, libraries));
                    }
                } catch (Exception e) {
                    log.debugf(e, "Prod UI: could not read extension metadata for %s", ext.toCompactCoords());
                }
            });
        }
        return result;
    }

    /** Normalize a yaml scalar-or-list value into a list of strings (null when absent). */
    static List<String> toStringList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        return List.of(String.valueOf(value));
    }

    /**
     * Parse a {@code lib-ga} metadata value ({@code groupId:artifactId[url]}) and resolve its version
     * from the application model. Returns null when the value is absent/unparseable or the artifact is
     * not on the model (mirrors Dev UI: only surface a badge when a real version is known).
     */
    private List<Library> resolveLibraries(String libGa, Map<String, String> versionMap) {
        if (libGa == null || libGa.isBlank()) {
            return null;
        }
        Matcher matcher = LIB_GA_PATTERN.matcher(libGa.trim());
        if (!matcher.matches()) {
            return null;
        }
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String url = matcher.group(4);
        String version = versionMap.get(groupId + ":" + artifactId);
        if (version == null) {
            return null;
        }
        return List.of(new Library(artifactId, version, url));
    }

    static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** Status may be a single string or a list; join lists the way Dev UI does. */
    static String collectionToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    private void generateAndEmbedBuildTimeDataFiles(List<ProdUIPageBuildItem> pages,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            String resourcePrefix,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) {

        for (ProdUIPageBuildItem page : pages) {
            String namespace = page.getExtensionPathName(curateOutcomeBuildItem);
            if (page.hasBuildTimeData()) {
                StringBuilder dataJs = new StringBuilder();
                for (Map.Entry<String, BuildTimeData> entry : page.getBuildTimeData().entrySet()) {
                    try {
                        String value = io.vertx.core.json.Json.encode(entry.getValue().getContent());
                        dataJs.append("export const ").append(entry.getKey()).append(" = ").append(value).append(";\n");
                    } catch (Exception ex) {
                        log.errorf(ex, "Could not serialize build-time data for Prod UI: %s", entry.getKey());
                    }
                }
                String dataFileName = namespace + "-data.js";
                embedResource(resourcePrefix + dataFileName, dataJs.toString().getBytes(StandardCharsets.UTF_8),
                        generatedResourceProducer, nativeImageResourceProducer);
            }
        }
    }

    private String generateIndexHtml(String contextRoot, String resourcePrefix) {
        try {
            URL templateUrl = Thread.currentThread().getContextClassLoader()
                    .getResource("prod-ui-templates/build-time/index.html");
            if (templateUrl == null) {
                log.warn("Prod UI index.html template not found");
                return "<html><body>Prod UI template not found</body></html>";
            }
            String template;
            try (InputStream is = templateUrl.openStream()) {
                template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            return template
                    .replace("{contextRoot}", contextRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Prod UI index.html", e);
        }
    }

    private List<String> collectEndpoints(IndexView index) {
        List<String> endpoints = new ArrayList<>();
        DotName pathAnnotation = DotName.createSimple("jakarta.ws.rs.Path");
        DotName getAnnotation = DotName.createSimple("jakarta.ws.rs.GET");
        DotName postAnnotation = DotName.createSimple("jakarta.ws.rs.POST");
        DotName putAnnotation = DotName.createSimple("jakarta.ws.rs.PUT");
        DotName deleteAnnotation = DotName.createSimple("jakarta.ws.rs.DELETE");

        for (AnnotationInstance annotation : index.getAnnotations(pathAnnotation)) {
            if (annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.CLASS) {
                ClassInfo classInfo = annotation.target().asClass();
                String basePath = annotation.value().asString();
                if (!basePath.startsWith("/")) {
                    basePath = "/" + basePath;
                }

                for (MethodInfo method : classInfo.methods()) {
                    StringBuilder methods = new StringBuilder();
                    AnnotationInstance methodPath = method.annotation(pathAnnotation);
                    String fullPath = basePath;
                    if (methodPath != null) {
                        String sub = methodPath.value().asString();
                        if (!sub.startsWith("/")) {
                            fullPath += "/" + sub;
                        } else {
                            fullPath += sub;
                        }
                    }

                    if (method.hasAnnotation(getAnnotation))
                        methods.append("GET ");
                    if (method.hasAnnotation(postAnnotation))
                        methods.append("POST ");
                    if (method.hasAnnotation(putAnnotation))
                        methods.append("PUT ");
                    if (method.hasAnnotation(deleteAnnotation))
                        methods.append("DELETE ");

                    String httpMethods = methods.toString().trim();
                    if (!httpMethods.isEmpty()) {
                        endpoints.add(fullPath + "|" + httpMethods);
                    }
                }
            }
        }
        return endpoints;
    }

    private String generateDependenciesDataJs(CurateOutcomeBuildItem curateOutcome, boolean excludeSelf) {
        StringBuilder nodes = new StringBuilder("[");
        StringBuilder links = new StringBuilder("[");
        String rootId = curateOutcome.getApplicationModel().getAppArtifact().toCompactCoords();

        nodes.append("{\"id\":\"").append(escapeJson(rootId)).append("\",");
        nodes.append("\"name\":\"").append(escapeJson(curateOutcome.getApplicationModel().getAppArtifact().getArtifactId()))
                .append("\"}");

        boolean firstNode = false;
        boolean firstLink = true;
        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
            // Prod UI reflects what is actually running in production, so only runtime-classpath jars belong here.
            // Deployment-only artifacts exist purely at build time and are not present in the running application.
            if (!dep.isRuntimeCp()) {
                continue;
            }
            // exclude-self: keep Prod UI's own modules out of the dependencies view
            if (excludeSelf && ProdUISelfFilter.isSelfArtifact(dep.getGroupId(), dep.getArtifactId())) {
                continue;
            }
            String id = dep.toCompactCoords();
            nodes.append(",{\"id\":\"").append(escapeJson(id)).append("\",");
            nodes.append("\"name\":\"").append(escapeJson(dep.getArtifactId())).append("\"}");

            if (!firstLink) {
                links.append(",");
            }
            firstLink = false;
            links.append("{\"source\":\"").append(escapeJson(rootId)).append("\",");
            links.append("\"target\":\"").append(escapeJson(id)).append("\",");
            links.append("\"direct\":true}");
        }
        nodes.append("]");
        links.append("]");

        return "export const dependencies = {\"rootId\":\"" + escapeJson(rootId)
                + "\",\"nodes\":" + nodes + ",\"links\":" + links + "};\n";
    }

    private String generateAppInfoJs() {
        org.eclipse.microprofile.config.Config config = org.eclipse.microprofile.config.ConfigProvider.getConfig();
        String appName = config.getOptionalValue("quarkus.application.name", String.class).orElse("");
        String appVersion = config.getOptionalValue("quarkus.application.version", String.class).orElse("");
        String quarkusVersion = Version.getVersion();

        return "export const applicationName = \"" + escapeJson(appName) + "\";\n"
                + "export const applicationVersion = \"" + escapeJson(appVersion) + "\";\n"
                + "export const quarkusVersion = \"" + escapeJson(quarkusVersion) + "\";\n";
    }

    /**
     * Returns true if the given JSON-RPC provider class declares at least one
     * method usable from the Prod UI (annotated {@code @JsonRpcUsage} containing
     * {@link Usage#PROD_UI}). Uses reflection on the runtime-retained annotation so
     * it can run before the combined index is available (see registerBeans).
     */
    private boolean hasProdUIMethod(Class<?> clazz) {
        for (java.lang.reflect.Method method : clazz.getMethods()) {
            JsonRpcUsage usage = method.getAnnotation(JsonRpcUsage.class);
            if (usage != null) {
                for (Usage u : usage.value()) {
                    if (u == Usage.PROD_UI) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private EnumSet<Usage> resolveUsage(MethodInfo method) {
        AnnotationInstance usageAnnotation = method.annotation(DotName.createSimple(JsonRpcUsage.class));
        if (usageAnnotation != null) {
            String[] usageArray = usageAnnotation.value().asEnumArray();
            EnumSet<Usage> usage = EnumSet.noneOf(Usage.class);
            for (String usageStr : usageArray) {
                usage.add(Usage.valueOf(usageStr));
            }
            return usage;
        }

        AnnotationInstance descAnnotation = method.annotation(DotName.createSimple(JsonRpcDescription.class));
        if (descAnnotation != null) {
            return Usage.devUIandDevMCP();
        }

        return Usage.onlyDevUI();
    }

    private String toJsonArray(List<String> items) {
        return "[" + items.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]";
    }

    private String formatTitle(String namespace) {
        String name = namespace;
        if (name.startsWith("quarkus-")) {
            name = name.substring("quarkus-".length());
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1).replace('-', ' ');
    }

    static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Class<?> toClass(Type type) {
        switch (type.kind()) {
            case PRIMITIVE:
                return switch (type.asPrimitiveType().primitive()) {
                    case BOOLEAN -> boolean.class;
                    case BYTE -> byte.class;
                    case CHAR -> char.class;
                    case DOUBLE -> double.class;
                    case FLOAT -> float.class;
                    case INT -> int.class;
                    case LONG -> long.class;
                    case SHORT -> short.class;
                };
            default:
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(type.name().toString());
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
        }
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // best effort
                        }
                    });
        } catch (IOException e) {
            // best effort
        }
    }
}
