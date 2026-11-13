package io.quarkus.gradle.tooling;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
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

public class ProjectDescriptorBuilder {

    private static final String ISOLATED_PROJECTS_PROPERTY = "org.gradle.unsafe.isolated-projects";

    public static Provider<DefaultProjectDescriptor> buildForApp(Project project) {
        final ProjectDescriptorBuilder builder = new ProjectDescriptorBuilder(project);
        project.afterEvaluate(evaluated -> ProjectDescriptorBuilder.initSourceDirs(evaluated, builder.moduleBuilder));
        // Collecting the workspace modules of the other projects requires cross-project access,
        // which is not allowed with Isolated Projects.
        // In that case, dependencies on other projects of the build won't have workspace module information.
        final boolean collectProjectModules = !isolatedProjectsEnabled(project.getProviders());
        return project.getProviders().provider(() -> new DefaultProjectDescriptor(builder.moduleBuilder,
                collectProjectModules ? builder.getProjectModules(project) : Map.of()));
    }

    private static boolean isolatedProjectsEnabled(ProviderFactory providers) {
        return Boolean.parseBoolean(providers.systemProperty(ISOLATED_PROJECTS_PROPERTY)
                .orElse(providers.gradleProperty(ISOLATED_PROJECTS_PROPERTY))
                .getOrElse("false"));
    }

    /**
     * Builds the workspace modules of the other Java projects of the build, keyed by project path.
     * <p>
     * This method is expected to be called lazily, once all the projects of the task graph have been evaluated,
     * so that the source directories of the projects the application depends on are known.
     * Projects that have not been evaluated at this point (e.g. with configuration on demand) are not part of
     * the build and are skipped.
     * <p>
     * Only the projects the application actually depends on are used by the application model task,
     * which matches them by project path.
     */
    private synchronized Map<String, WorkspaceModule.Mutable> getProjectModules(Project project) {
        if (projectModules != null) {
            return projectModules;
        }
        final Map<String, WorkspaceModule.Mutable> result = new HashMap<>();
        for (Project p : project.getRootProject().getAllprojects()) {
            if (p == project
                    || !p.getState().getExecuted()
                    || p.getExtensions().findByType(SourceSetContainer.class) == null) {
                continue;
            }
            final WorkspaceModule.Mutable module = newModule(p);
            initSourceDirs(p, module);
            result.put(p.getPath(), module);
        }
        projectModules = result;
        return result;
    }

    public static void initSourceDirs(Project project, WorkspaceModule.Mutable result) {
        final SourceSetContainer srcSets = project.getExtensions().getByType(SourceSetContainer.class);
        // Here we are iterating through the JARs that will be produced, collecting directories that will be used as sources
        // of their content. Then we are figuring out which source directories would be processed to produce the content of the JARs.
        // It has to be configureEach instead of forEach, apparently to avoid concurrent collection modification in some cases.
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
                            sourceDirs.add(new LazySourceDir(srcDir.toPath(), classesDir.toPath())); // TODO findGeneratedSourceDir(destDir, sourceSet));
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

        // This is for the test sources and resources since, by default, they won't be put in JARs
        project.getTasks().withType(Test.class).configureEach(testTask -> {
            for (SourceSet srcSet : srcSets) {
                String classifier = null;
                List<SourceDir> testSourcesDirs = Collections.emptyList();
                List<SourceDir> testResourcesDirs = Collections.emptyList();
                for (var classesDir : srcSet.getOutput().getClassesDirs().getFiles()) {
                    if (testTask.getTestClassesDirs().contains(classesDir)) {
                        if (classifier == null) {
                            classifier = sourceSetNameToClassifier(srcSet.getName());
                            if (result.hasSources(classifier)) {
                                // this source set should already be present in the module
                                break;
                            }
                        }
                        for (var srcDir : srcSet.getAllJava().getSrcDirs()) {
                            if (testSourcesDirs.isEmpty()) {
                                testSourcesDirs = new ArrayList<>(6);
                            }
                            testSourcesDirs.add(new LazySourceDir(srcDir.toPath(), classesDir.toPath())); // TODO findGeneratedSourceDir(destDir, sourceSet));
                        }
                    }
                }
                if (classifier != null && !testSourcesDirs.isEmpty()) {
                    if (srcSet.getOutput().getResourcesDir() != null) {
                        final Path resourcesOutputDir = srcSet.getOutput().getResourcesDir().toPath();
                        for (var dir : srcSet.getResources().getSrcDirs()) {
                            if (testResourcesDirs.isEmpty()) {
                                testResourcesDirs = new ArrayList<>(2);
                            }
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

    private static WorkspaceModule.Mutable newModule(Project project) {
        return WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of(String.valueOf(project.getGroup()), project.getName(),
                        String.valueOf(project.getVersion())))
                .setModuleDir(project.getLayout().getProjectDirectory().getAsFile().toPath())
                .setBuildDir(project.getLayout().getBuildDirectory().get().getAsFile().toPath())
                .setBuildFile(project.getBuildFile().toPath());
    }

    private final WorkspaceModule.Mutable moduleBuilder;

    /**
     * Workspace modules of the other projects of the build, initialized lazily
     */
    private Map<String, WorkspaceModule.Mutable> projectModules;

    private ProjectDescriptorBuilder(Project project) {
        this.moduleBuilder = newModule(project);
    }
}
