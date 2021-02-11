package io.quarkus.gradle.tasks;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.attributes.Category;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Internal;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactKey;
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

        final List<Dependency> boms = boms();
        if (boms.isEmpty()) {
            throw new GradleException("No platforms detected in the project");
        }

        final QuarkusJsonPlatformDescriptorResolver platformResolver = QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setArtifactResolver(extension().getAppModelResolver())
                .setMessageWriter(new GradleMessageWriter(getProject().getLogger()));

        final QuarkusPlatformDescriptor platform = resolvePlatformDescriptor(platformResolver, getProject(), boms);
        if (platform != null) {
            return platform;
        }

        final List<QuarkusPlatformDescriptor> platforms = resolveLegacyPlatforms(platformResolver, boms);
        if (platforms.isEmpty()) {
            throw new GradleException("Failed to determine the Quarkus platform for the project");
        }
        if (platforms.size() == 1) {
            return platforms.get(0);
        }

        final CombinedQuarkusPlatformDescriptor.Builder builder = CombinedQuarkusPlatformDescriptor.builder();
        for (QuarkusPlatformDescriptor descriptor : platforms) {
            builder.addPlatform(descriptor);
        }
        return builder.build();
    }

    private List<Dependency> boms() {
        final Configuration impl = getProject().getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        List<Dependency> boms = new ArrayList<>();
        impl.getIncoming().getDependencies()
                .forEach(d -> {
                    if (!(d instanceof ModuleDependency)) {
                        return;
                    }
                    final ModuleDependency module = (ModuleDependency) d;
                    final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
                    if (category != null
                            && (Category.ENFORCED_PLATFORM.equals(category.getName())
                                    || Category.REGULAR_PLATFORM.equals(category.getName()))) {
                        boms.add(d);
                    }
                });
        return boms;
    }

    private List<QuarkusPlatformDescriptor> resolveLegacyPlatforms(
            QuarkusJsonPlatformDescriptorResolver platformResolver,
            List<Dependency> boms) {
        List<QuarkusPlatformDescriptor> platforms = new ArrayList<>(2);
        boms.forEach(bom -> {
            try {
                platforms
                        .add(platformResolver.resolveFromBom(bom.getGroup(), bom.getName(), bom.getVersion()));
            } catch (Exception e) {
                // not a platform
            }
        });
        return platforms;
    }

    private QuarkusPlatformDescriptor resolvePlatformDescriptor(QuarkusJsonPlatformDescriptorResolver descriptorResolver,
            Project project, List<Dependency> bomDeps) {
        final Configuration boms = project.getConfigurations()
                .detachedConfiguration(bomDeps.toArray(new org.gradle.api.artifacts.Dependency[0]));
        final Set<AppArtifactKey> processedKeys = new HashSet<>(1);
        final List<ResolvedArtifact> descriptorDeps = new ArrayList<>(2);
        boms.getResolutionStrategy().eachDependency(d -> {
            if (!d.getTarget().getName().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)
                    || !processedKeys.add(new AppArtifactKey(d.getTarget().getGroup(), d.getTarget().getName()))) {
                return;
            }

            final DefaultDependencyArtifact dep = new DefaultDependencyArtifact();
            dep.setExtension("json");
            dep.setType("json");
            dep.setClassifier(d.getTarget().getVersion());
            dep.setName(d.getTarget().getName());

            final DefaultExternalModuleDependency gradleDep = new DefaultExternalModuleDependency(
                    d.getTarget().getGroup(), d.getTarget().getName(), d.getTarget().getVersion(), null);
            gradleDep.addArtifact(dep);

            try {
                for (ResolvedArtifact a : project.getConfigurations().detachedConfiguration(gradleDep)
                        .getResolvedConfiguration().getResolvedArtifacts()) {
                    if (a.getName().equals(d.getTarget().getName())) {
                        descriptorDeps.add(a);
                        break;
                    }
                }
            } catch (Exception e) {
                // ignore for now
            }
        });
        boms.getResolvedConfiguration();

        if (descriptorDeps.isEmpty()) {
            return null;
        }
        if (descriptorDeps.size() == 1) {
            return descriptorResolver.resolveFromJson(descriptorDeps.get(0).getFile().toPath());
        }

        // Typically, quarkus-bom platform will appear first.
        // The descriptors that are generated today are not fragmented and include everything
        // a platform offers. Which means if the quarkus-bom platform appears first and its version
        // matches the Quarkus core version of the platform built on top of the quarkus-bom
        // (e.g. quarkus-universe-bom) the quarkus-bom platform can be skipped,
        // since it will already be included in the platform that's built on top of it
        int i = 0;
        ResolvedArtifact platformArtifact = descriptorDeps.get(0);
        final String quarkusBomPlatformArtifactId = "quarkus-bom-"
                + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX;
        ResolvedArtifact quarkusBomPlatformArtifact = null;
        if (quarkusBomPlatformArtifactId.equals(platformArtifact.getName())) {
            quarkusBomPlatformArtifact = platformArtifact;
        }
        final CombinedQuarkusPlatformDescriptor.Builder builder = CombinedQuarkusPlatformDescriptor.builder();
        while (++i < descriptorDeps.size()) {
            platformArtifact = descriptorDeps.get(i);
            final QuarkusPlatformDescriptor descriptor = descriptorResolver
                    .resolveFromJson(platformArtifact.getFile().toPath());
            if (quarkusBomPlatformArtifact != null) {
                if (!quarkusBomPlatformArtifact.getModuleVersion().getId().getVersion()
                        .equals(descriptor.getQuarkusVersion())) {
                    builder.addPlatform(descriptorResolver.resolveFromJson(quarkusBomPlatformArtifact.getFile().toPath()));
                }
                quarkusBomPlatformArtifact = null;
            }
            builder.addPlatform(descriptorResolver.resolveFromJson(platformArtifact.getFile().toPath()));
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
