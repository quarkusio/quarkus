package io.quarkus.vertx.http.deployment.webjar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.runtime.ApplicationConfig;

/**
 * Utility for Web resource related operations
 */
public class WebJarUtil {

    private static final Logger LOG = Logger.getLogger(WebJarUtil.class);

    private static final String CUSTOM_MEDIA_FOLDER = "META-INF/branding/";
    private static final List<String> OVERRIDABLE_RESOURCES = Arrays.asList("logo.png", "favicon.ico", "style.css");

    private WebJarUtil() {
    }

    static Path copyResourcesForDevOrTest(CurateOutcomeBuildItem curateOutcomeBuildItem, ApplicationConfig config,
            WebJarBuildItem webJar, ResolvedDependency resourcesArtifact, Path deploymentBasePath) throws IOException {

        Path deploymentPath = Files.createDirectories(deploymentBasePath);

        PathTargetVisitor visitor = new PathTargetVisitor(deploymentPath);
        copyResources(curateOutcomeBuildItem, config, webJar, resourcesArtifact, visitor);

        return deploymentPath;
    }

    static Map<String, byte[]> copyResourcesForProduction(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ApplicationConfig config, WebJarBuildItem webJar, ResolvedDependency resourcesArtifact) {

        InMemoryTargetVisitor visitor = new InMemoryTargetVisitor();
        copyResources(curateOutcomeBuildItem, config, webJar, resourcesArtifact, visitor);

        return visitor.getContent();
    }

    private static void copyResources(CurateOutcomeBuildItem curateOutcomeBuildItem, ApplicationConfig config,
            WebJarBuildItem webJar, ResolvedDependency resourcesArtifact, WebJarResourcesTargetVisitor visitor) {
        final ResolvedDependency userApplication = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();

        ClassLoader classLoader = WebJarUtil.class.getClassLoader();

        resourcesArtifact.getContentTree().accept(webJar.getRoot(), new Consumer<PathVisit>() {
            @Override
            public void accept(PathVisit pathVisit) {
                if (pathVisit == null || !Files.isDirectory(pathVisit.getPath())) {
                    return;
                }

                List<WebJarResourcesFilter> filters = new ArrayList<>();
                if (webJar.getFilter() != null) {
                    filters.add(webJar.getFilter());
                }
                filters.add(new InsertVariablesResourcesFilter(config, userApplication));

                try {
                    Files.walkFileTree(pathVisit.getPath(),
                            new ResourcesFileVisitor(visitor, pathVisit.getPath(), resourcesArtifact, userApplication,
                                    new CombinedWebJarResourcesFilter(filters), classLoader, webJar));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    static ResolvedDependency getAppArtifact(CurateOutcomeBuildItem curateOutcomeBuildItem, GACT artifactKey) {
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            if (dep.getKey().equals(artifactKey)) {
                return dep;
            }
        }
        throw new RuntimeException("Could not find artifact " + artifactKey + " among the application dependencies");
    }

    private static String getModuleOverrideName(ResolvedDependency artifact, String filename) {
        String type = filename.substring(filename.lastIndexOf("."));
        return artifact.getArtifactId() + type;
    }

    private static InputStream getOverride(ResolvedDependency userApplication, ClassLoader classLoader, String filename,
            String moduleName, boolean useDefaultQuarkusBranding) {

        // First check if the developer supplied the files
        InputStream overrideStream = getCustomOverride(userApplication, filename, moduleName);
        if (overrideStream == null && useDefaultQuarkusBranding) {
            // Else check if Quarkus has a default branding
            overrideStream = getQuarkusOverride(classLoader, filename, moduleName);
        }
        return overrideStream;
    }

    private static InputStream getCustomOverride(ResolvedDependency userApplication, String filename,
            String moduleName) {
        // Check if the developer supplied the files
        byte[] content = readFromPathTree(userApplication.getContentTree(), CUSTOM_MEDIA_FOLDER + moduleName);
        if (content != null) {
            return new ByteArrayInputStream(content);
        }

        content = readFromPathTree(userApplication.getContentTree(), CUSTOM_MEDIA_FOLDER + filename);
        if (content != null) {
            return new ByteArrayInputStream(content);
        }

        return null;
    }

    private static byte[] readFromPathTree(PathTree tree, String relativePath) {
        return tree.apply(relativePath, (visit) -> {
            if (visit == null) {
                return null;
            }

            try {
                return Files.readAllBytes(visit.getPath());
            } catch (IOException e) {
                LOG.error("Could not read file content " + visit.getPath(), e);
            }
            return null;
        });
    }

    private static InputStream getQuarkusOverride(ClassLoader classLoader, String filename, String moduleName) {
        // Allow quarkus per module override
        InputStream stream = classLoader.getResourceAsStream(CUSTOM_MEDIA_FOLDER + moduleName);
        if (stream != null) {
            return stream;
        }

        return classLoader.getResourceAsStream(CUSTOM_MEDIA_FOLDER + filename);
    }

    private static class ResourcesFileVisitor extends SimpleFileVisitor<Path> {
        private final WebJarResourcesTargetVisitor visitor;
        private final Path rootFolderToCopy;
        private final ResolvedDependency resourcesArtifact;
        private final ResolvedDependency userApplication;
        private final WebJarResourcesFilter filter;
        private final ClassLoader classLoader;
        private final WebJarBuildItem webJar;

        public ResourcesFileVisitor(WebJarResourcesTargetVisitor visitor, Path rootFolderToCopy,
                ResolvedDependency resourcesArtifact, ResolvedDependency userApplication, WebJarResourcesFilter filter,
                ClassLoader classLoader, WebJarBuildItem webJar) {
            this.visitor = visitor;
            this.rootFolderToCopy = rootFolderToCopy;
            this.resourcesArtifact = resourcesArtifact;
            this.userApplication = userApplication;
            this.filter = filter;
            this.classLoader = classLoader;
            this.webJar = webJar;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            visitor.visitDirectory(rootFolderToCopy.relativize(dir).toString());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            String fileName = rootFolderToCopy.relativize(file).toString();

            String moduleName = getModuleOverrideName(resourcesArtifact, fileName);
            boolean overrideFileCreated = false;
            if (OVERRIDABLE_RESOURCES.contains(fileName)) {
                try (WebJarResourcesFilter.FilterResult filterResult = filter.apply(fileName, getOverride(
                        userApplication, classLoader, fileName, moduleName, webJar.getUseDefaultQuarkusBranding()))) {
                    if (filterResult.hasStream()) {
                        overrideFileCreated = true;
                        // Override (either developer supplied or Quarkus)
                        visitor.visitFile(fileName, filterResult.getStream());
                    }
                }
            }

            if (!overrideFileCreated) {
                try (WebJarResourcesFilter.FilterResult filterResult = filter.apply(fileName,
                        Files.newInputStream(file))) {
                    if (!visitor.supportsOnlyCopyingNonArtifactFiles() || !webJar.getOnlyCopyNonArtifactFiles()
                            || filterResult.isChanged()) {
                        if (filterResult.hasStream()) {
                            visitor.visitFile(fileName, filterResult.getStream());
                        }
                    }
                }
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
