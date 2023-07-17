package io.quarkus.webjar.locator.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

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

public class WebJarLocatorStandaloneBuildStep {

    private static final String WEBJARS_PREFIX = "META-INF/resources/webjars";
    private static final Logger log = Logger.getLogger(WebJarLocatorStandaloneBuildStep.class.getName());

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findWebjarsAndCreateHandler(
            HttpBuildTimeConfig httpConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            CurateOutcomeBuildItem curateOutcome,
            WebJarLocatorRecorder recorder) throws Exception {

        final List<ClassPathElement> providers = QuarkusClassLoader.getElements(WEBJARS_PREFIX, false);
        Map<String, String> webjarNameToVersionMap = Collections.emptyMap();
        if (!providers.isEmpty()) {
            final Map<ArtifactKey, ClassPathElement> webJarKeys = new HashMap<>(providers.size());
            for (ClassPathElement provider : providers) {
                if (provider.getDependencyKey() == null || !provider.isRuntime()) {
                    log.warn("webjars content found in " + provider.getRoot()
                            + " won't be available. Please, report this issue.");
                } else {
                    webJarKeys.put(provider.getDependencyKey(), provider);
                }
            }
            if (!webJarKeys.isEmpty()) {
                final Map<String, String> webjarMap = new HashMap<>(webJarKeys.size());
                for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
                    if (!dep.isRuntimeCp()) {
                        continue;
                    }
                    final ClassPathElement provider = webJarKeys.get(dep.getKey());
                    if (provider == null) {
                        continue;
                    }
                    provider.apply(tree -> {
                        final Path webjarsDir = tree.getPath(WEBJARS_PREFIX);
                        final Path nameDir;
                        try (Stream<Path> webjarsDirPaths = Files.list(webjarsDir)) {
                            nameDir = webjarsDirPaths.filter(Files::isDirectory).findFirst().get();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        if (nameDir == null) {
                            log.warn("Failed to determine the name for webjars included in "
                                    + tree.getOriginalTree().getRoots());
                            return null;
                        }
                        final String version = Files.isDirectory(nameDir.resolve(dep.getVersion())) ? dep.getVersion() : null;
                        webjarMap.put(nameDir.getFileName().toString(), version);
                        return null;
                    });
                }
                webjarNameToVersionMap = webjarMap;
            }
        }

        if (!webjarNameToVersionMap.isEmpty()) {
            // The context path + the resources path
            String rootPath = httpConfig.rootPath;
            String webjarRootPath = (rootPath.endsWith("/")) ? rootPath + "webjars/" : rootPath + "/webjars/";
            feature.produce(new FeatureBuildItem(Feature.WEBJARS_LOCATOR));
            routes.produce(
                    RouteBuildItem.builder().route("/webjars/*")
                            .handler(recorder.getHandler(webjarRootPath, webjarNameToVersionMap)).build());
        } else {
            log.warn("No WebJars were found in the project. Requests to the /webjars/ path will always return 404 (Not Found)");
        }
    }
}
