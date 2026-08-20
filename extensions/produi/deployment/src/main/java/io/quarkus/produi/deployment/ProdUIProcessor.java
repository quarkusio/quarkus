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
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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
import io.quarkus.builder.Version;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeData;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.produi.runtime.ProdUIBuildTimeConfig;
import io.quarkus.produi.runtime.ProdUIRecorder;
import io.quarkus.produi.runtime.config.ConfigProdUIService;
import io.quarkus.produi.runtime.endpoints.EndpointsProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;

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

    @BuildStep
    void registerBuiltInJsonRPCProviders(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProviders) {
        jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", ConfigProdUIService.class));
        jsonRPCProviders.produce(new JsonRPCProvidersBuildItem("quarkus-produi", EndpointsProdUIService.class));
    }

    @BuildStep(onlyIf = IsProduction.class)
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexProducer,
            ProdUIBuildTimeConfig config,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {

        if (!config.enabled()) {
            return;
        }

        for (JsonRPCProvidersBuildItem provider : jsonRPCProvidersBuildItems) {
            Class<?> clazz = provider.getJsonRPCMethodProviderClass();
            additionalIndexProducer.produce(new AdditionalIndexedClassesBuildItem(clazz.getName()));

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

        discoverProdUIMethods(index, curateOutcomeBuildItem, jsonRPCProvidersBuildItems,
                runtimeMethods, runtimeSubscriptions, methodNames, subscriptionNames);

        // 2. Initialize the JsonRPC router and register endpoints
        recorder.initializeJsonRpcRouter(beanContainer.getValue(), runtimeMethods, runtimeSubscriptions);

        List<String> endpointPaths = collectEndpoints(index);
        recorder.registerEndpoints(beanContainer.getValue(), endpointPaths);

        // 3. Generate dynamic data files
        String pagesDataJs = generatePagesDataJs(pages, curateOutcomeBuildItem);
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
        String depsDataJs = generateDependenciesDataJs(curateOutcomeBuildItem);
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
                "prod-ui/pui/pui-page-host.js",
                "prod-ui/pui/pui-configuration.js",
                "prod-ui/pui/pui-endpoints.js",
                "prod-ui/pui/pui-dependencies.js",
                "prod-ui/shims/jsonrpc.js",
                "prod-ui/shims/localization.js"
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

    private void discoverProdUIMethods(IndexView index,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JsonRPCProvidersBuildItem> providers,
            Map<String, JsonRpcMethod> runtimeMethods,
            Map<String, JsonRpcMethod> runtimeSubscriptions,
            List<String> methodNames,
            List<String> subscriptionNames) {

        for (JsonRPCProvidersBuildItem provider : providers) {
            Class<?> clazz = provider.getJsonRPCMethodProviderClass();
            String extension = provider.getExtensionPathName(curateOutcomeBuildItem);
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
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        // Built-in pages
        String[][] builtInPages = {
                { "Configuration", "pui-configuration" },
                { "Endpoints", "pui-endpoints" },
                { "Dependencies", "pui-dependencies" }
        };
        for (String[] bp : builtInPages) {
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

        // Extension-contributed pages
        for (ProdUIPageBuildItem page : pages) {
            String namespace = page.getExtensionPathName(curateOutcomeBuildItem);
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("{\"namespace\":\"").append(escapeJson(namespace)).append("\",");
            json.append("\"title\":\"").append(escapeJson(formatTitle(namespace))).append("\",");
            json.append("\"pages\":[");
            boolean firstPage = true;
            for (var pageBuilder : page.getPages()) {
                var p = pageBuilder.build();
                if (!firstPage) {
                    json.append(",");
                }
                firstPage = false;
                json.append("{\"title\":\"").append(escapeJson(p.getTitle())).append("\",");
                json.append("\"componentLink\":\"").append(escapeJson(p.getComponentLink())).append("\"}");
            }
            json.append("]}");
        }
        json.append("]");
        return "export const pages = " + json + ";\n";
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

    private String generateDependenciesDataJs(CurateOutcomeBuildItem curateOutcome) {
        StringBuilder nodes = new StringBuilder("[");
        StringBuilder links = new StringBuilder("[");
        String rootId = curateOutcome.getApplicationModel().getAppArtifact().toCompactCoords();

        nodes.append("{\"id\":\"").append(escapeJson(rootId)).append("\",");
        nodes.append("\"name\":\"").append(escapeJson(curateOutcome.getApplicationModel().getAppArtifact().getArtifactId()))
                .append("\"}");

        boolean firstNode = false;
        boolean firstLink = true;
        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
            String id = dep.toCompactCoords();
            nodes.append(",{\"id\":\"").append(escapeJson(id)).append("\",");
            nodes.append("\"name\":\"").append(escapeJson(dep.getArtifactId())).append("\"}");

            if (!firstLink) {
                links.append(",");
            }
            firstLink = false;
            links.append("{\"source\":\"").append(escapeJson(rootId)).append("\",");
            links.append("\"target\":\"").append(escapeJson(id)).append("\",");
            links.append("\"type\":\"").append(dep.isRuntimeCp() ? "runtime" : "deployment").append("\",");
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

    private String escapeJson(String s) {
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
