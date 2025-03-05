package io.quarkus.webdependency.locator.deployment.devui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;

public class WebDependencyLocatorDevModeApiProcessor {

    private static final String PREFIX = "META-INF/resources/";
    private static final String WEBJARS_PATH = "webjars";
    private static final String MVNPM_PATH = "_static";

    private static final Logger log = Logger.getLogger(WebDependencyLocatorDevModeApiProcessor.class.getName());

    @BuildStep(onlyIf = IsDevelopment.class)
    public void findWebDependenciesAssets(
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<WebDependencyLibrariesBuildItem> webDependencyLibrariesProducer) {

        final List<WebDependencyLibrary> webJarLibraries = getLibraries(httpBuildTimeConfig, curateOutcome, WEBJARS_PATH);
        webDependencyLibrariesProducer.produce(new WebDependencyLibrariesBuildItem("webjars", webJarLibraries));

        final List<WebDependencyLibrary> mvnpmLibraries = getLibraries(httpBuildTimeConfig, curateOutcome, MVNPM_PATH);
        webDependencyLibrariesProducer.produce(new WebDependencyLibrariesBuildItem("mvnpm", mvnpmLibraries));

    }

    private List<WebDependencyLibrary> getLibraries(
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            CurateOutcomeBuildItem curateOutcome, String path) {
        final List<WebDependencyLibrary> webDependencyLibraries = new ArrayList<>();
        final List<ClassPathElement> providers = QuarkusClassLoader.getElements(PREFIX + path, false);
        if (!providers.isEmpty()) {
            // Map of webDependency artifact keys to class path elements
            final Map<ArtifactKey, ClassPathElement> webDependencyKeys = providers.stream()
                    .filter(provider -> provider.getDependencyKey() != null && provider.isRuntime())
                    .collect(Collectors.toMap(ClassPathElement::getDependencyKey, provider -> provider, (a, b) -> b,
                            () -> new HashMap<>(providers.size())));
            if (!webDependencyKeys.isEmpty()) {
                // The root path of the application
                final String rootPath = httpBuildTimeConfig.rootPath();
                // The root path of the webDependencies
                final String webDependencyRootPath = (rootPath.endsWith("/")) ? rootPath + path + "/"
                        : rootPath + "/" + path + "/";

                // For each packaged web dependency, create a WebDependencyLibrary object
                curateOutcome.getApplicationModel().getDependencies().stream()
                        .map(dep -> createWebDependencyLibrary(dep, webDependencyRootPath, webDependencyKeys, path))
                        .filter(Objects::nonNull).forEach(webDependencyLibraries::add);
            }
        }
        return webDependencyLibraries;
    }

    private WebDependencyLibrary createWebDependencyLibrary(ResolvedDependency dep,
            String webDependencyRootPath,
            Map<ArtifactKey, ClassPathElement> webDependencyKeys,
            String path) {
        // If the dependency is not a runtime class path dependency, return null
        if (!dep.isRuntimeCp()) {
            return null;
        }
        final ClassPathElement provider = webDependencyKeys.get(dep.getKey());
        if (provider == null) {
            return null;
        }
        final WebDependencyLibrary webDependencyLibrary = new WebDependencyLibrary(provider.getDependencyKey().getArtifactId());
        provider.apply(tree -> {
            final Path webDependenciesDir = tree.getPath(PREFIX + path);
            final Path nameDir;
            try (Stream<Path> webDependenciesDirPaths = Files.list(webDependenciesDir)) {
                nameDir = webDependenciesDirPaths.filter(Files::isDirectory).findFirst().orElseThrow(() -> new IOException(
                        "Could not find name directory for " + dep.getKey().getArtifactId() + " in " + webDependenciesDir));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            final Path versionDir;
            Path root = nameDir;
            // The base URL for the Web Dependency
            final StringBuilder urlBase = new StringBuilder(webDependencyRootPath);
            boolean appendRootPart = true;
            try {
                // If the version directory exists, use it as a root, otherwise use the name directory
                versionDir = nameDir.resolve(dep.getVersion());
                root = Files.isDirectory(versionDir) ? versionDir : nameDir;
                urlBase.append(nameDir.getFileName().toString())
                        .append("/");
                appendRootPart = false;
            } catch (InvalidPathException e) {
                log.warn("Could not find version directory for " + dep.getKey().getArtifactId() + " "
                        + dep.getVersion() + " in " + nameDir + ", falling back to name directory");
            }
            webDependencyLibrary.setVersion(dep.getVersion());
            try {
                // Create the asset tree for the web dependency and set it as the root asset
                var asset = createAssetForLibrary(root, urlBase.toString(), appendRootPart);
                webDependencyLibrary.setRootAsset(asset);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return null;
        });

        return webDependencyLibrary;
    }

    private WebDependencyAsset createAssetForLibrary(Path rootPath, String urlBase, boolean appendRootPart)
            throws IOException {
        //If it is a directory, go deeper, otherwise add the file
        var root = new WebDependencyAsset();
        root.setName(rootPath.getFileName().toString());
        root.setChildren(new LinkedList<>());
        root.setFileAsset(false);
        urlBase = appendRootPart ? urlBase + root.getName() + "/" : urlBase;

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootPath)) {
            for (Path childPath : directoryStream) {
                if (Files.isDirectory(childPath)) { // If it is a directory, go deeper, otherwise add the file
                    var childDir = createAssetForLibrary(childPath, urlBase, true);
                    root.getChildren().add(childDir);
                } else {
                    var childFile = new WebDependencyAsset();
                    childFile.setName(childPath.getFileName().toString());
                    childFile.setFileAsset(true);
                    childFile.setUrlPart(urlBase + childFile.getName());
                    root.getChildren().add(childFile);
                }
            }
        }
        // Sort the children by name
        root.getChildren().sort(Comparator.comparing(WebDependencyAsset::getName));
        return root;
    }

}
