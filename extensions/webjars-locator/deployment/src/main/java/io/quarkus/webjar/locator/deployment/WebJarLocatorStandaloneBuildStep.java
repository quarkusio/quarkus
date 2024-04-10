package io.quarkus.webjar.locator.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.webjar.locator.runtime.WebJarLocatorRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class WebJarLocatorStandaloneBuildStep {

    private static final String WEBJARS_PREFIX = "META-INF/resources/webjars";
    private static final String WEBJARS_NAME = "webjars";

    private static final String MVNPM_PREFIX = "META-INF/resources/_static";
    private static final String MVNPM_NAME = "mvnpm";

    private static final Logger log = Logger.getLogger(WebJarLocatorStandaloneBuildStep.class.getName());

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findWebjarsAndCreateHandler(
            WebJarLocatorConfig config,
            HttpBuildTimeConfig httpConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<ImportMapBuildItem> im,
            CurateOutcomeBuildItem curateOutcome,
            WebJarLocatorRecorder recorder) throws Exception {

        LibInfo webjarsLibInfo = getLibInfo(curateOutcome, WEBJARS_PREFIX, WEBJARS_NAME);
        LibInfo mvnpmNameLibInfo = getLibInfo(curateOutcome, MVNPM_PREFIX, MVNPM_NAME);

        if (webjarsLibInfo != null || mvnpmNameLibInfo != null) {
            feature.produce(new FeatureBuildItem(Feature.WEBJARS_LOCATOR));

            if (webjarsLibInfo != null) {
                if (config.versionReroute) {
                    Handler<RoutingContext> handler = recorder.getHandler(getRootPath(httpConfig, "webjars"),
                            webjarsLibInfo.nameVersionMap);
                    routes.produce(RouteBuildItem.builder().route("/webjars/*").handler(handler).build());
                }
            } else {
                log.warn(
                        "No WebJars were found in the project. Requests to the /webjars/ path will always return 404 (Not Found)");
            }
            if (mvnpmNameLibInfo != null) {
                if (config.versionReroute) {
                    Handler<RoutingContext> handler = recorder.getHandler(getRootPath(httpConfig, "_static"),
                            mvnpmNameLibInfo.nameVersionMap);
                    routes.produce(RouteBuildItem.builder().route("/_static/*").handler(handler).build());
                }
                // Also create a importmap endpoint
                Aggregator aggregator = new Aggregator(mvnpmNameLibInfo.jars);
                if (!config.importMappings.isEmpty()) {
                    aggregator.addMappings(config.importMappings);
                }

                String importMap = aggregator.aggregateAsJson(false);
                im.produce(new ImportMapBuildItem(importMap));
                String path = getRootPath(httpConfig, IMPORTMAP_ROOT) + IMPORTMAP_FILENAME;
                Handler<RoutingContext> importMapHandler = recorder.getImportMapHandler(path,
                        importMap);
                routes.produce(
                        RouteBuildItem.builder().route("/" + IMPORTMAP_ROOT + "/" + IMPORTMAP_FILENAME)
                                .handler(importMapHandler).build());
            } else {
                log.warn(
                        "No Mvnpm jars were found in the project. Requests to the /_static/ path will always return 404 (Not Found)");
            }
        }
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

    private String getRootPath(HttpBuildTimeConfig httpConfig, String path) {
        // The context path + the resources path
        String rootPath = httpConfig.rootPath;
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

    private static final String IMPORTMAP_ROOT = "_importmap";
    private static final String IMPORTMAP_FILENAME = "generated_importmap.js";
}
