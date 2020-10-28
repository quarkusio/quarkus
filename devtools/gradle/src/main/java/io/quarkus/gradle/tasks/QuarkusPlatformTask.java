package io.quarkus.gradle.tasks;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Internal;

import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.buildfile.BuildFile;
import io.quarkus.devtools.project.buildfile.GradleGroovyProjectBuildFile;
import io.quarkus.devtools.project.buildfile.GradleKotlinProjectBuildFile;
import io.quarkus.platform.descriptor.CombinedQuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;

public abstract class QuarkusPlatformTask extends QuarkusTask {

    QuarkusPlatformTask(String description) {
        super(description);
    }

    protected QuarkusPlatformDescriptor platformDescriptor() {

        final QuarkusJsonPlatformDescriptorResolver platformResolver = QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setArtifactResolver(extension().getAppModelResolver())
                .setMessageWriter(new GradleMessageWriter(getProject().getLogger()));

        List<QuarkusPlatformDescriptor> platforms = new ArrayList<>(2);
        final Configuration impl = getProject().getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        impl.getIncoming().getDependencies()
                .forEach(d -> {
                    if (!(d instanceof ModuleDependency)) {
                        return;
                    }
                    final ModuleDependency module = (ModuleDependency) d;
                    final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
                    if (category == null || !Category.ENFORCED_PLATFORM.equals(category.getName())) {
                        return;
                    }
                    try {
                        platforms
                                .add(platformResolver.resolveFromBom(module.getGroup(), module.getName(), module.getVersion()));
                    } catch (Exception e) {
                        // not a platform
                    }
                });

        if (platforms.isEmpty()) {
            throw new GradleException("Failed to determine Quarkus platform for the project");
        }

        if (platforms.size() == 1) {
            return platforms.get(0);
        }

        final CombinedQuarkusPlatformDescriptor.Builder builder = CombinedQuarkusPlatformDescriptor.builder();
        for (QuarkusPlatformDescriptor platform : platforms) {
            builder.addPlatform(platform);
        }
        return builder.build();
    }

    @Internal
    protected QuarkusProject getQuarkusProject() {
        final QuarkusPlatformDescriptor platformDescriptor = platformDescriptor();
        final Path projectDirPath = getProject().getProjectDir().toPath();
        final Path rootProjectPath = getProject().getParent() != null ? getProject().getRootProject().getProjectDir().toPath()
                : projectDirPath;
        final BuildFile buildFile;
        if (Files.exists(rootProjectPath.resolve("settings.gradle.kts"))
                && Files.exists(projectDirPath.resolve("build.gradle.kts"))) {
            buildFile = new GradleKotlinProjectBuildFile(getProject(), platformDescriptor);
        } else if (Files.exists(rootProjectPath.resolve("settings.gradle"))
                && Files.exists(projectDirPath.resolve("build.gradle"))) {
            buildFile = new GradleGroovyProjectBuildFile(getProject(), platformDescriptor);
        } else {
            throw new GradleException(
                    "Mixed DSL is not supported. Both build and settings file need to use either Kotlin or Groovy DSL");
        }
        return QuarkusProject.of(getProject().getProjectDir().toPath(), platformDescriptor, buildFile);
    }

    protected static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new GradleException("Malformed URL:" + url, e);
        }
    }

}
