package io.quarkus.gradle.tasks;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Internal;

import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
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
    protected GradleBuildFile getGradleBuildFile() {
        return getProject().getParent() == null
                ? new GradleBuildFile(getWriter())
                : new GradleBuildFile(getWriter(),
                        new FileProjectWriter(getProject().getRootProject().getProjectDir()));
    }

    @Internal
    protected FileProjectWriter getWriter() {
        return new FileProjectWriter(getProject().getProjectDir());
    }
}
