package io.quarkus.devtools.project.buildfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public abstract class GradleProjectBuildFile extends AbstractGradleBuildFile {

    private final Project project;

    public GradleProjectBuildFile(Project project, QuarkusPlatformDescriptor platformDescriptor) {
        super(project.getProjectDir().toPath(), platformDescriptor,
                project.getParent() != null ? project.getRootProject().getProjectDir().toPath()
                        : project.getProjectDir().toPath());
        this.project = project;
    }

    @Override
    protected List<AppArtifactCoords> getDependencies() throws IOException {

        final Set<Dependency> boms = new HashSet<>();
        // collect enforced platforms
        final Configuration impl = project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        for (Dependency d : impl.getAllDependencies()) {
            if (!(d instanceof ModuleDependency)) {
                continue;
            }
            final ModuleDependency module = (ModuleDependency) d;
            final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
            if (category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                    || Category.REGULAR_PLATFORM.equals(category.getName()))) {
                boms.add(d);
            }
        }

        final Set<ResolvedArtifact> resolvedArtifacts = project.getConfigurations()
                .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).getResolvedConfiguration()
                .getResolvedArtifacts();
        final List<AppArtifactCoords> coords = new ArrayList<>(boms.size() + resolvedArtifacts.size());
        boms.forEach(d -> {
            coords.add(new AppArtifactCoords(d.getGroup(), d.getName(), null, "pom", d.getVersion()));
        });
        resolvedArtifacts.forEach(a -> {
            coords.add(new AppArtifactCoords(a.getModuleVersion().getId().getGroup(), a.getName(),
                    a.getClassifier(), a.getExtension(), a.getModuleVersion().getId().getVersion()));
        });
        return coords;
    }
}
