package io.quarkus.gradle.tooling;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.bundling.Jar;

import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.LazySourceDir;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

public class ProjectDescriptorBuilder {

    public static Provider<DefaultProjectDescriptor> buildForApp(Project project) {
        final ProjectDescriptorBuilder builder = new ProjectDescriptorBuilder(project);
        project.afterEvaluate(evaluated -> ProjectDescriptorBuilder.initSourceDirs(evaluated, builder.moduleBuilder));
        return project.getProviders().provider(() -> new DefaultProjectDescriptor(builder.moduleBuilder));
    }

    public static void initSourceDirs(Project project, WorkspaceModule.Mutable result) {
        final SourceSetContainer srcSets = project.getExtensions().getByType(SourceSetContainer.class);
        // Here we are checking JARs will be produced, which directories they will use as the source of content
        // and figure out which source directories are processed to produce the content of the JARs.
        // It has to be configureEach instead of forEach, apparently to avoid concurrent collection modification in some cases.
        project.getTasks().withType(Jar.class).configureEach(jarTask -> {
            final String classifier = jarTask.getArchiveClassifier().get();

            final List<File> classesDirs = new ArrayList<>(2);
            final List<File> resourcesOutputDirs = new ArrayList<>(2);
            collectSourceSetOutput(((DefaultCopySpec) jarTask.getRootSpec()), classesDirs, resourcesOutputDirs);

            final List<SourceDir> sourceDirs = new ArrayList<>(2);
            final List<SourceDir> resourceDirs = new ArrayList<>(2);
            for (SourceSet srcSet : srcSets) {
                for (var classesDir : srcSet.getOutput().getClassesDirs().getFiles()) {
                    if (classesDirs.contains(classesDir)) {
                        for (var srcDir : srcSet.getAllJava().getSrcDirs()) {
                            sourceDirs.add(new LazySourceDir(srcDir.toPath(), classesDir.toPath())); // TODO findGeneratedSourceDir(destDir, sourceSet));
                        }
                    }
                }

                final File resourcesOutputDir = srcSet.getOutput().getResourcesDir();
                if (resourcesOutputDirs.contains(resourcesOutputDir)) {
                    for (var dir : srcSet.getResources().getSrcDirs()) {
                        resourceDirs.add(new LazySourceDir(dir.toPath(), resourcesOutputDir.toPath()));
                    }
                }
            }

            if (!sourceDirs.isEmpty() || !resourceDirs.isEmpty()) {
                result.addArtifactSources(new DefaultArtifactSources(classifier, sourceDirs, resourceDirs));
            }
        });
    }

    private static void collectSourceSetOutput(DefaultCopySpec spec, List<File> classesDir, List<File> resourcesDir) {
        for (var paths : spec.getSourcePaths()) {
            if (paths instanceof SourceSetOutput sso) {
                classesDir.addAll(sso.getClassesDirs().getFiles());
                resourcesDir.add(sso.getResourcesDir());
            }
        }
        for (var child : spec.getChildren()) {
            collectSourceSetOutput((DefaultCopySpec) child, classesDir, resourcesDir);
        }
    }

    private final WorkspaceModule.Mutable moduleBuilder;

    private ProjectDescriptorBuilder(Project project) {
        this.moduleBuilder = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of(String.valueOf(project.getGroup()), project.getName(),
                        String.valueOf(project.getVersion())))
                .setModuleDir(project.getLayout().getProjectDirectory().getAsFile().toPath())
                .setBuildDir(project.getLayout().getBuildDirectory().get().getAsFile().toPath())
                .setBuildFile(project.getBuildFile().toPath());
    }
}
