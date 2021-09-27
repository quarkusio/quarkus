package io.quarkus.devtools.project.buildfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.util.PlatformArtifacts;

public abstract class GradleProjectBuildFile extends AbstractGradleBuildFile {

    private final Project project;

    public GradleProjectBuildFile(Project project, ExtensionCatalog catalog) {
        super(project.getProjectDir().toPath(), catalog,
                project.getParent() != null ? project.getRootProject().getProjectDir().toPath()
                        : project.getProjectDir().toPath());
        this.project = project;
    }

    @Override
    protected List<ArtifactCoords> getDependencies() throws IOException {

        final List<Dependency> boms = boms();

        final List<ArtifactCoords> coords = new ArrayList<>();
        boms.forEach(d -> {
            coords.add(new ArtifactCoords(d.getGroup(), d.getName(), null, "pom", d.getVersion()));
        });
        project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).getIncoming().getDependencies().forEach(d -> {
                    if (!(d instanceof ModuleDependency)) {
                        return;
                    }
                    final ModuleDependency module = (ModuleDependency) d;
                    coords.add(new ArtifactCoords(module.getGroup(), module.getName(), module.getVersion()));
                    // why is the following code does not return any artifact?
                    //module.getArtifacts().forEach(a -> {
                    //    coords.add(new ArtifactCoords(module.getGroup(), module.getName(), a.getClassifier(),
                    //            a.getExtension(), module.getVersion()));
                    //});
                });
        return coords;
    }

    @Override
    public String getProperty(String name) {
        final Object o = project.getProperties().get(name);
        return o == null ? null : o.toString();
    }

    protected ArtifactCoords toBomImportCoords(ArtifactCoords rawBomCoords) {
        if (rawBomCoords.getGroupId().equals(getProperty("quarkusPlatformGroupId"))
                && rawBomCoords.getVersion().equals(getProperty("quarkusPlatformVersion"))) {
            return new ArtifactCoords("${quarkusPlatformGroupId}",
                    rawBomCoords.getArtifactId().equals(getProperty("quarkusPlatformArtifactId"))
                            ? "${quarkusPlatformArtifactId}"
                            : rawBomCoords.getArtifactId(),
                    "pom",
                    "${quarkusPlatformVersion}");
        }
        return rawBomCoords;
    }

    private List<Dependency> boms() {
        final List<Dependency> boms = new ArrayList<>();
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
        return boms;
    }

    @Override
    public List<ArtifactCoords> getInstalledPlatforms() throws IOException {
        final List<Dependency> bomDeps = boms();
        if (bomDeps.isEmpty()) {
            return Collections.emptyList();
        }
        final Configuration boms = project.getConfigurations()
                .detachedConfiguration(bomDeps.toArray(new org.gradle.api.artifacts.Dependency[0]));
        final List<ArtifactCoords> platforms = new ArrayList<>();
        boms.getResolutionStrategy().eachDependency(d -> {
            if (!d.getTarget().getName().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                return;
            }
            final ArtifactCoords platform = new ArtifactCoords(d.getTarget().getGroup(),
                    PlatformArtifacts.ensureBomArtifactId(d.getTarget().getName()), null, "pom", d.getTarget().getVersion());
            platforms.add(platform);
        });
        boms.getResolvedConfiguration();
        return platforms;
    }
}
