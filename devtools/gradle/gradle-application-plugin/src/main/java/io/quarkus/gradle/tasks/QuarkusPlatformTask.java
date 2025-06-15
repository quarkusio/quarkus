package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.GradleUtils.listProjectBoms;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.buildfile.BuildFile;
import io.quarkus.devtools.project.buildfile.GradleGroovyProjectBuildFile;
import io.quarkus.devtools.project.buildfile.GradleKotlinProjectBuildFile;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

public abstract class QuarkusPlatformTask extends QuarkusTask {

    private volatile ExtensionCatalogResolver catalogResolver;

    QuarkusPlatformTask(String description) {
        super(description);
    }

    private ExtensionCatalog extensionsCatalog(boolean limitExtensionsToImportedPlatforms, MessageWriter log) {
        final List<ArtifactCoords> platforms = importedPlatforms();
        ExtensionCatalogResolver catalogResolver;
        catalogResolver = getExtensionCatalogResolver(log);
        if (catalogResolver.hasRegistries() && !limitExtensionsToImportedPlatforms) {
            try {
                return catalogResolver.resolveExtensionCatalog(platforms);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve extension catalog", e);
            }
        }
        return ToolsUtils.mergePlatforms(platforms, extension().getAppModelResolver());
    }

    protected ExtensionCatalogResolver getExtensionCatalogResolver(MessageWriter log) {
        if (catalogResolver == null) {
            try {
                catalogResolver = QuarkusProjectHelper.isRegistryClientEnabled()
                        ? QuarkusProjectHelper.getCatalogResolver(log)
                        : ExtensionCatalogResolver.empty();
            } catch (RegistryResolutionException e) {
                throw new RuntimeException("Failed to initialize Quarkus extension catalog resolver", e);
            }
        }
        return catalogResolver;
    }

    protected List<ArtifactCoords> importedPlatforms() {
        final List<Dependency> bomDeps = listProjectBoms(getProject());
        if (bomDeps.isEmpty()) {
            throw new GradleException("No platforms detected in the project");
        }

        final Configuration boms = getProject().getConfigurations()
                .detachedConfiguration(bomDeps.toArray(new org.gradle.api.artifacts.Dependency[0]));
        final Set<ArtifactKey> processedKeys = new HashSet<>(1);

        List<ArtifactCoords> platforms = new ArrayList<>();
        boms.getResolutionStrategy().eachDependency(d -> {
            if (!d.getTarget().getName().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)
                    || !processedKeys.add(ArtifactKey.ga(d.getTarget().getGroup(), d.getTarget().getName()))) {
                return;
            }
            final ArtifactCoords platform = ArtifactCoords.of(d.getTarget().getGroup(), d.getTarget().getName(),
                    d.getTarget().getVersion(), "json", d.getTarget().getVersion());
            platforms.add(platform);
        });
        boms.getResolvedConfiguration();

        if (platforms.isEmpty()) {
            throw new RuntimeException("No Quarkus platforms found in the project");
        }
        return platforms;
    }

    protected String quarkusCoreVersion() {
        final List<Dependency> bomDeps = listProjectBoms(getProject());
        if (bomDeps.isEmpty()) {
            throw new GradleException("No platforms detected in the project");
        }

        final Configuration boms = getProject().getConfigurations()
                .detachedConfiguration(bomDeps.toArray(new org.gradle.api.artifacts.Dependency[0]));

        final AtomicReference<String> quarkusVersionRef = new AtomicReference<>();
        boms.getResolutionStrategy().eachDependency(d -> {
            if (quarkusVersionRef.get() == null && d.getTarget().getName().equals("quarkus-core")
                    && d.getTarget().getGroup().equals("io.quarkus")) {
                quarkusVersionRef.set(d.getTarget().getVersion());
            }
        });
        boms.getResolvedConfiguration();
        final String quarkusCoreVersion = quarkusVersionRef.get();
        if (quarkusCoreVersion == null) {
            throw new IllegalStateException("Failed to determine the Quarkus core version for the project");
        }
        return quarkusCoreVersion;
    }

    protected QuarkusProject getQuarkusProject(boolean limitExtensionsToImportedPlatforms) {
        final GradleMessageWriter log = messageWriter();
        final ExtensionCatalog catalog = extensionsCatalog(limitExtensionsToImportedPlatforms, log);

        final Path projectDirPath = getProject().getProjectDir().toPath();
        final Path rootProjectPath = getProject().getParent() != null
                ? getProject().getRootProject().getProjectDir().toPath()
                : projectDirPath;
        final BuildFile buildFile;
        if (Files.exists(rootProjectPath.resolve("settings.gradle.kts"))
                && Files.exists(projectDirPath.resolve("build.gradle.kts"))) {
            buildFile = new GradleKotlinProjectBuildFile(getProject(), catalog);
        } else if (Files.exists(rootProjectPath.resolve("settings.gradle"))
                && Files.exists(projectDirPath.resolve("build.gradle"))) {
            buildFile = new GradleGroovyProjectBuildFile(getProject(), catalog);
        } else {
            throw new GradleException(
                    "Mixed DSL is not supported. Both build and settings file need to use either Kotlin or Groovy DSL");
        }
        final JavaVersion javaVersion = resolveProjectJavaVersion();
        return QuarkusProjectHelper.getProject(getProject().getProjectDir().toPath(), catalog, buildFile, javaVersion,
                log);
    }

    private JavaVersion resolveProjectJavaVersion() {
        TaskCollection<JavaCompile> compileTasks = getProject().getTasks().withType(JavaCompile.class);
        if (compileTasks.isEmpty()) {
            return JavaVersion.NA;
        }
        final JavaCompile task = compileTasks.iterator().next();
        return new JavaVersion(task.getTargetCompatibility());
    }

    protected GradleMessageWriter messageWriter() {
        return new GradleMessageWriter(getLogger());
    }

    protected static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new GradleException("Malformed URL:" + url, e);
        }
    }

}
