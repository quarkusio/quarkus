package io.quarkus.webdependency.locator.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.mvnpm.importmap.Aggregator;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.webdependency.locator.runtime.WebDependencyLocatorRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class WebDependencyLocatorProcessor {
    private static final Logger log = Logger.getLogger(WebDependencyLocatorProcessor.class.getName());

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.WEB_DEPENDENCY_LOCATOR));
    }

    @BuildStep
    public void findRelevantFiles(BuildProducer<GeneratedStaticResourceBuildItem> generatedStaticProducer,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedProducer,
            WebDependencyLocatorConfig config) throws IOException {

        QuarkusClassLoader.visitRuntimeResources(config.webRoot(), visit -> {
            final Path web = visit.getPath();
            if (Files.isDirectory(web)) {
                hotDeploymentWatchedProducer
                        .produce(new HotDeploymentWatchedFileBuildItem(config.webRoot() + SLASH + STAR + STAR));
                // Find all css and js (under /app)
                Path app = web
                        .resolve(config.appRoot());

                List<Path> cssFiles = new ArrayList<>();
                List<Path> jsFiles = new ArrayList<>();

                if (Files.exists(app)) {
                    hotDeploymentWatchedProducer
                            .produce(new HotDeploymentWatchedFileBuildItem(
                                    config.webRoot() + SLASH + config.appRoot() + SLASH + STAR + STAR));
                    try (Stream<Path> appstream = Files.walk(app)) {
                        appstream.forEach(path -> {
                            if (Files.isRegularFile(path) && path.toString().endsWith(DOT_CSS)) {
                                cssFiles.add(web.relativize(path));
                            } else if (Files.isRegularFile(path) && path.toString().endsWith(DOT_JS)) {
                                jsFiles.add(web.relativize(path));
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                try (Stream<Path> webstream = Files.walk(web)) {

                    webstream.forEach(path -> {
                        if (Files.isRegularFile(path)) {
                            String endpoint = SLASH + web.relativize(path);
                            endpoint = endpoint.replace('\\', '/');
                            try {
                                if (path.toString().endsWith(DOT_HTML)) {
                                    generatedStaticProducer.produce(new GeneratedStaticResourceBuildItem(endpoint,
                                            processHtml(path, cssFiles, jsFiles)));
                                } else {
                                    generatedStaticProducer.produce(new GeneratedStaticResourceBuildItem(endpoint, path));
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });

    }

    private byte[] processHtml(
            Path path, List<Path> cssFiles, List<Path> jsFiles)
            throws IOException {
        StringJoiner modifiedContent = new StringJoiner(System.lineSeparator());

        Files.lines(path).forEach(line -> {
            String modifiedLine = processLine(line, cssFiles, jsFiles);
            modifiedContent.add(modifiedLine);
        });

        String result = modifiedContent.toString();
        return result.getBytes();
    }

    private static String processLine(String line, List<Path> cssFiles, List<Path> jsFiles) {
        if (line.contains(HASH_IMPORTMAP)) {
            line = TAB2 + IMPORTMAP_REPLACEMENT;
        }

        // Let's also support web-bundler style
        if (line.contains(HASH_BUNDLE)) {
            try (StringWriter sw = new StringWriter()) {
                if (!cssFiles.isEmpty()) {
                    for (Path css : cssFiles) {
                        sw.write(TAB2 + "<link href='" + css.toString().replace("\\", "/") + "' rel='stylesheet'>"
                                + System.lineSeparator());
                    }
                }
                sw.write(TAB2 + IMPORTMAP_REPLACEMENT);
                if (!jsFiles.isEmpty()) {
                    sw.write(System.lineSeparator());
                    sw.write(TAB2 + "<script type='module'>" + System.lineSeparator());
                    for (Path js : jsFiles) {
                        sw.write(TAB2 + TAB + "import '" + js.toString().replace("\\", "/") + "';" + System.lineSeparator());
                    }
                    sw.write(TAB2 + "</script>");
                }
                line = sw.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return line;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findWebDependenciesAndCreateHandler(
            WebDependencyLocatorConfig config,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<ImportMapBuildItem> im,
            CurateOutcomeBuildItem curateOutcome,
            WebDependencyLocatorRecorder recorder) throws Exception {

        LibInfo webjarsLibInfo = getLibInfo(curateOutcome, WEBJARS_PREFIX, WEBJARS_NAME);
        LibInfo mvnpmNameLibInfo = getLibInfo(curateOutcome, MVNPM_PREFIX, MVNPM_NAME);

        if (webjarsLibInfo != null || mvnpmNameLibInfo != null) {

            if (webjarsLibInfo != null) {
                if (config.versionReroute()) {
                    routes.produce(
                            createRouteBuildItem(recorder, httpBuildTimeConfig, WEBJARS_PATH, webjarsLibInfo.nameVersionMap));
                }
            }
            if (mvnpmNameLibInfo != null) {
                if (config.versionReroute()) {
                    routes.produce(
                            createRouteBuildItem(recorder, httpBuildTimeConfig, MVNPM_PATH, mvnpmNameLibInfo.nameVersionMap));
                }
                // Also create a importmap endpoint
                Aggregator aggregator = new Aggregator(mvnpmNameLibInfo.jars);
                Map<String, String> importMappings = config.importMappings();
                if (!importMappings.containsKey(config.appRoot() + SLASH)) {
                    // Add default for app/
                    importMappings.put(config.appRoot() + SLASH, SLASH + config.appRoot() + SLASH);
                }
                if (!config.importMappings().isEmpty()) {
                    aggregator.addMappings(config.importMappings());
                }

                String importMap = aggregator.aggregateAsJson(false);
                im.produce(new ImportMapBuildItem(importMap));
                String path = getRootPath(httpBuildTimeConfig, IMPORTMAP_ROOT) + IMPORTMAP_FILENAME;
                Handler<RoutingContext> importMapHandler = recorder.getImportMapHandler(path,
                        importMap);
                routes.produce(
                        RouteBuildItem.builder().route(SLASH + IMPORTMAP_ROOT + SLASH + IMPORTMAP_FILENAME)
                                .handler(importMapHandler).build());
            }
        } else {
            log.warn(
                    "No WebJars or mvnpm jars were found in the project. Requests to the /webjars/ and/or /_static/ path will always return 404 (Not Found)");
        }

    }

    private RouteBuildItem createRouteBuildItem(WebDependencyLocatorRecorder recorder,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            String path, Map<String, String> nameVersionMap) {
        Handler<RoutingContext> handler = recorder.getHandler(getRootPath(httpBuildTimeConfig, path),
                nameVersionMap);
        return RouteBuildItem.builder().route(SLASH + path + SLASH + STAR).handler(handler).build();
    }

    private LibInfo getLibInfo(CurateOutcomeBuildItem curateOutcome, String prefix, String name) {

        final List<ClassPathElement> providers = QuarkusClassLoader.getElements(prefix, false);
        if (!providers.isEmpty()) {
            final Map<ArtifactKey, ClassPathElement> keys = new HashMap<>(providers.size());
            for (ClassPathElement provider : providers) {
                if (provider.getDependencyKey() != null && provider.isRuntime()) {
                    keys.put(provider.getDependencyKey(), provider);
                }
            }
            if (!keys.isEmpty()) {
                final Map<String, String> map = new HashMap<>(keys.size());
                final Set<URL> jars = new HashSet<>();
                for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
                    if (!dep.isRuntimeCp()) {
                        continue;
                    }

                    final ClassPathElement provider = keys.get(dep.getKey());
                    if (provider == null) {
                        continue;
                    }
                    provider.apply(tree -> {
                        final Path dir = tree.getPath(prefix);
                        final Path nameDir;
                        try (Stream<Path> dirPaths = Files.list(dir)) {
                            nameDir = dirPaths.filter(Files::isDirectory).findFirst().get();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        if (nameDir == null) {
                            log.warn("Failed to determine the name for " + name + " included in "
                                    + tree.getOriginalTree().getRoots());
                            return null;
                        }
                        final String version = Files.isDirectory(nameDir.resolve(dep.getVersion())) ? dep.getVersion() : null;
                        map.put(nameDir.getFileName().toString(), version);
                        try {
                            jars.add(dep.getResolvedPaths().getSinglePath().toUri().toURL());
                        } catch (MalformedURLException ex) {
                            throw new RuntimeException(ex);
                        }
                        return null;
                    });
                }

                return new LibInfo(map, jars);
            }
        }
        return null;
    }

    private String getRootPath(VertxHttpBuildTimeConfig httpBuildTimeConfig, String path) {
        // The context path + the resources path
        String rootPath = httpBuildTimeConfig.rootPath();
        return (rootPath.endsWith("/")) ? rootPath + path + "/" : rootPath + "/" + path + "/";
    }

    static class LibInfo {
        Map<String, String> nameVersionMap;
        Set<URL> jars;

        LibInfo(Map<String, String> nameVersionMap, Set<URL> jars) {
            this.nameVersionMap = nameVersionMap;
            this.jars = jars;
        }

    }

    private static final String WEBJARS_PREFIX = "META-INF/resources/webjars";
    private static final String WEBJARS_NAME = "webjars";
    private static final String WEBJARS_PATH = "webjars";

    private static final String MVNPM_PREFIX = "META-INF/resources/_static";
    private static final String MVNPM_NAME = "mvnpm";
    private static final String MVNPM_PATH = "_static";

    private static final String IMPORTMAP_ROOT = "_importmap";
    private static final String IMPORTMAP_FILENAME = "generated_importmap.js";

    private static final String HASH_BUNDLE = "#bundle";
    private static final String HASH_IMPORTMAP = "#importmap";
    private static final String IMPORTMAP_REPLACEMENT = "<script src='/_importmap/generated_importmap.js'></script>";
    private static final String TAB = "\t";
    private static final String TAB2 = TAB + TAB;

    private static final String SLASH = "/";
    private static final String STAR = "*";

    private static final String DOT_HTML = ".html";
    private static final String DOT_CSS = ".css";
    private static final String DOT_JS = ".js";
}
