package io.quarkus.gradle.model.config;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.LazySourceDir;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;

public final class CurrentProjectDescriptorBuilder {

    private CurrentProjectDescriptorBuilder() {
    }

    public static Provider<DefaultProjectDescriptor> buildForCurrentProject(Project project) {
        WorkspaceModule.Mutable module = moduleBuilder(project);
        initModuleAfterEvaluation(project, module);
        return project.getProviders().provider(() -> {
            refreshModuleId(project, module);
            return new DefaultProjectDescriptor(module);
        });
    }

    private static WorkspaceModule.Mutable moduleBuilder(Project project) {
        return WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of(String.valueOf(project.getGroup()), project.getName(),
                        String.valueOf(project.getVersion())))
                .setModuleDir(project.getLayout().getProjectDirectory().getAsFile().toPath())
                .setBuildDir(project.getLayout().getBuildDirectory().get().getAsFile().toPath())
                .setBuildFile(project.getBuildFile().toPath());
    }

    private static void initModuleAfterEvaluation(Project project, WorkspaceModule.Mutable module) {
        if (project.getState().getExecuted()) {
            refreshModuleId(project, module);
            initSourceDirs(project, module);
        } else {
            project.afterEvaluate(evaluated -> {
                refreshModuleId(evaluated, module);
                initSourceDirs(evaluated, module);
            });
        }
    }

    private static void refreshModuleId(Project project, WorkspaceModule.Mutable module) {
        module.setModuleId(WorkspaceModuleId.of(String.valueOf(project.getGroup()), project.getName(),
                String.valueOf(project.getVersion())));
    }

    private static void initSourceDirs(Project project, WorkspaceModule.Mutable result) {
        final SourceSetContainer srcSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (srcSets == null) {
            return;
        }
        project.getTasks().withType(Jar.class).configureEach(jarTask -> {
            final String classifier = jarTask.getArchiveClassifier().get();

            final List<File> classesDirs = new ArrayList<>(2);
            final List<File> resourcesOutputDirs = new ArrayList<>(2);
            collectSourceSetOutput(((DefaultCopySpec) jarTask.getRootSpec()), classesDirs, resourcesOutputDirs);

            final List<SourceDir> sourceDirs = new ArrayList<>();
            final List<SourceDir> resourceDirs = new ArrayList<>(2);
            for (SourceSet srcSet : srcSets) {
                for (var classesDir : srcSet.getOutput().getClassesDirs().getFiles()) {
                    if (classesDirs.contains(classesDir)) {
                        for (var srcDir : srcSet.getAllJava().getSrcDirs()) {
                            sourceDirs.add(new LazySourceDir(srcDir.toPath(), classesDir.toPath(),
                                    findGeneratedSourceDir(classesDir, srcSet)));
                        }
                    }
                }

                if (resourcesOutputDirs.contains(srcSet.getOutput().getResourcesDir())) {
                    var resourcesTarget = srcSet.getOutput().getResourcesDir().toPath();
                    for (var dir : srcSet.getResources().getSrcDirs()) {
                        resourceDirs.add(new LazySourceDir(dir.toPath(), resourcesTarget));
                    }
                }
            }

            if (!sourceDirs.isEmpty() || !resourceDirs.isEmpty()) {
                result.addArtifactSources(new DefaultArtifactSources(classifier, sourceDirs, resourceDirs));
            }
        });

        project.getTasks().withType(Test.class).configureEach(testTask -> {
            for (SourceSet srcSet : srcSets) {
                String classifier = null;
                List<SourceDir> testSourcesDirs = new ArrayList<>(6);
                List<SourceDir> testResourcesDirs = new ArrayList<>(2);
                for (var classesDir : srcSet.getOutput().getClassesDirs().getFiles()) {
                    if (testTask.getTestClassesDirs().contains(classesDir)) {
                        if (classifier == null) {
                            classifier = sourceSetNameToClassifier(srcSet.getName());
                            if (result.hasSources(classifier)) {
                                break;
                            }
                        }
                        for (var srcDir : srcSet.getAllJava().getSrcDirs()) {
                            testSourcesDirs.add(new LazySourceDir(srcDir.toPath(), classesDir.toPath(),
                                    findGeneratedSourceDir(classesDir, srcSet)));
                        }
                    }
                }
                if (classifier != null && !testSourcesDirs.isEmpty()) {
                    if (srcSet.getOutput().getResourcesDir() != null) {
                        final Path resourcesOutputDir = srcSet.getOutput().getResourcesDir().toPath();
                        for (var dir : srcSet.getResources().getSrcDirs()) {
                            testResourcesDirs.add(new LazySourceDir(dir.toPath(), resourcesOutputDir));
                        }
                    }
                    result.addArtifactSources(new DefaultArtifactSources(classifier, testSourcesDirs, testResourcesDirs));
                }
            }
        });
    }

    private static String sourceSetNameToClassifier(String sourceSetName) {
        if (SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSetName)) {
            return ArtifactSources.TEST;
        }
        var sb = new StringBuilder(sourceSetName.length() + 2);
        for (int i = 0; i < sourceSetName.length(); ++i) {
            char original = sourceSetName.charAt(i);
            char lowerCase = Character.toLowerCase(original);
            if (original != lowerCase) {
                sb.append('-');
            }
            sb.append(lowerCase);
        }
        return sb.toString();
    }

    private static Path findGeneratedSourceDir(File classesDir, SourceSet sourceSet) {
        if (classesDir.getParentFile() == null) {
            return null;
        }
        String language = classesDir.getParentFile().getName();
        String sourceSetName = classesDir.getName();
        for (File generatedDir : sourceSet.getOutput().getGeneratedSourcesDirs().getFiles()) {
            if (generatedDir.getParentFile() == null) {
                continue;
            }
            if (generatedDir.getName().equals(sourceSetName)
                    && generatedDir.getParentFile().getName().equals(language)) {
                return generatedDir.toPath();
            }
        }
        return null;
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
}
