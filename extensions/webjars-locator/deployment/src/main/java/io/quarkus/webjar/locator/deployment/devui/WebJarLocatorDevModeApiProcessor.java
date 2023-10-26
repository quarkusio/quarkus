package io.quarkus.webjar.locator.deployment.devui;

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
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

public class WebJarLocatorDevModeApiProcessor {

    private static final String WEBJARS_PREFIX = "META-INF/resources/webjars";
    private static final Logger log = Logger.getLogger(WebJarLocatorDevModeApiProcessor.class.getName());

    @BuildStep(onlyIf = IsDevelopment.class)
    public void findWebjarsAssets(
            HttpBuildTimeConfig httpConfig,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<WebJarLibrariesBuildItem> webJarLibrariesProducer) {

        final List<WebJarLibrary> webJarLibraries = new ArrayList<>();
        final List<ClassPathElement> providers = QuarkusClassLoader.getElements(WEBJARS_PREFIX, false);
        if (!providers.isEmpty()) {
            // Map of webjar artifact keys to class path elements
            final Map<ArtifactKey, ClassPathElement> webJarKeys = providers.stream()
                    .filter(provider -> provider.getDependencyKey() != null && provider.isRuntime())
                    .collect(Collectors.toMap(ClassPathElement::getDependencyKey, provider -> provider, (a, b) -> b,
                            () -> new HashMap<>(providers.size())));
            if (!webJarKeys.isEmpty()) {
                // The root path of the application
                final String rootPath = httpConfig.rootPath;
                // The root path of the webjars
                final String webjarRootPath = (rootPath.endsWith("/")) ? rootPath + "webjars/" : rootPath + "/webjars/";

                // For each packaged webjar dependency, create a WebJarLibrary object
                curateOutcome.getApplicationModel().getDependencies().stream()
                        .map(dep -> createWebJarLibrary(dep, webjarRootPath, webJarKeys))
                        .filter(Objects::nonNull).forEach(webJarLibraries::add);
            }
        }
        webJarLibrariesProducer.produce(new WebJarLibrariesBuildItem(webJarLibraries));
    }

    private WebJarLibrary createWebJarLibrary(ResolvedDependency dep, String webjarRootPath,
            Map<ArtifactKey, ClassPathElement> webJarKeys) {
        // If the dependency is not a runtime class path dependency, return null
        if (!dep.isRuntimeCp()) {
            return null;
        }
        final ClassPathElement provider = webJarKeys.get(dep.getKey());
        if (provider == null) {
            return null;
        }
        final WebJarLibrary webJarLibrary = new WebJarLibrary(provider.getDependencyKey().getArtifactId());
        provider.apply(tree -> {
            final Path webjarsDir = tree.getPath(WEBJARS_PREFIX);
            final Path nameDir;
            try (Stream<Path> webjarsDirPaths = Files.list(webjarsDir)) {
                nameDir = webjarsDirPaths.filter(Files::isDirectory).findFirst().orElseThrow(() -> new IOException(
                        "Could not find name directory for " + dep.getKey().getArtifactId() + " in " + webjarsDir));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            final Path versionDir;
            Path root = nameDir;
            // The base URL for the webjar
            final StringBuilder urlBase = new StringBuilder(webjarRootPath);
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
            webJarLibrary.setVersion(dep.getVersion());
            try {
                // Create the asset tree for the webjar and set it as the root asset
                var asset = createAssetForLibrary(root, urlBase.toString(), appendRootPart);
                webJarLibrary.setRootAsset(asset);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return null;
        });

        return webJarLibrary;
    }

    private WebJarAsset createAssetForLibrary(Path rootPath, String urlBase, boolean appendRootPart)
            throws IOException {
        //If it is a directory, go deeper, otherwise add the file
        var root = new WebJarAsset();
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
                    var childFile = new WebJarAsset();
                    childFile.setName(childPath.getFileName().toString());
                    childFile.setFileAsset(true);
                    childFile.setUrlPart(urlBase + childFile.getName());
                    root.getChildren().add(childFile);
                }
            }
        }
        // Sort the children by name
        root.getChildren().sort(Comparator.comparing(WebJarAsset::getName));
        return root;
    }

}
