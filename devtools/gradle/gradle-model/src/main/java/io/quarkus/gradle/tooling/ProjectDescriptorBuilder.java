package io.quarkus.gradle.tooling;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
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
import io.quarkus.maven.dependency.GAV;

public class ProjectDescriptorBuilder {

    public static Provider<DefaultProjectDescriptor> buildForApp(Project project, Configuration runtimeConfiguration,
            Configuration deploymentConfiguration) {
        final ProjectDescriptorBuilder builder = new ProjectDescriptorBuilder(project);
        // Map<GAV, File> pomFilesHolder = new HashMap<>();
        // final Map<String, List<DependencyInfoCollector.DeclaredDependency>> declaredDepsHolder = new HashMap<>();
        final Map<GAV, Model> declaredDepsByModuleGAV = new HashMap<>();
        final Map<String, List<DependencyInfoCollector.DeclaredDependency>> declaredDepsByProjectPath = new HashMap<>();
        Action<Project> projectAction = proj -> {
            ProjectDescriptorBuilder.initSourceDirs(proj, builder.moduleBuilder);
            GradleAssistedMavenModelResolverImpl mavenModelResolver = new GradleAssistedMavenModelResolverImpl(project);
            DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
            collectResolvedModuleAndProjectDependencies(project, deploymentConfiguration, mavenModelResolver, modelBuilder,
                    declaredDepsByModuleGAV, declaredDepsByProjectPath);
            collectResolvedModuleAndProjectDependencies(project, runtimeConfiguration, mavenModelResolver, modelBuilder,
                    declaredDepsByModuleGAV, declaredDepsByProjectPath);
            System.out.println("[DEBUG] Collected declared dependencies for projects: " + proj.getPath()); // TODO: remove
        };
        if (project.getState().getExecuted()) {
            projectAction.execute(project);
        } else {
            project.afterEvaluate(projectAction);
        }
        return project.getProviders().provider(() -> {
            System.out.println("[DEBUG] Creating ProjectDescriptor for project: " + project.getPath()); // TODO: remove
            return new DefaultProjectDescriptor(builder.moduleBuilder, project.getPath(),
                    declaredDepsByModuleGAV, declaredDepsByProjectPath);
        });
    }

    private static void collectResolvedModuleAndProjectDependencies(
            Project project,
            Configuration configuration,
            GradleAssistedMavenModelResolverImpl mavenModelResolver,
            DefaultModelBuilder modelBuilder,
            Map<GAV, Model> declaredDepsByModuleGAV,
            Map<String, List<DependencyInfoCollector.DeclaredDependency>> declaredDepsByProjectPath) {
        Set<ResolvedComponentResult> components = configuration.getIncoming().getResolutionResult()
                .getAllComponents();

        long totalTimeForModelResolution = 0;

        for (ResolvedComponentResult component : components) {
            if (component.getId() instanceof ModuleComponentIdentifier moduleId) {
                String groupId = moduleId.getGroup();
                String artifactId = moduleId.getModule();
                String version = moduleId.getVersion();
                Model effectiveModel;
                // resolve the POM to get the file
                try {
                    GAV key = new GAV(groupId, artifactId, version);
                    if (declaredDepsByModuleGAV.containsKey(key)) {
                        continue;
                    }
                    var modelSource = mavenModelResolver.resolveModel(groupId, artifactId, version);
                    ModelBuildingRequest request = new DefaultModelBuildingRequest();
                    request.setModelSource(modelSource);
                    request.setModelResolver(mavenModelResolver);
                    request.getSystemProperties().putAll(System.getProperties());
                    request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                    var startTime = System.currentTimeMillis();
                    effectiveModel = modelBuilder.build(request).getEffectiveModel();
                    var endTime = System.currentTimeMillis();
                    long l = endTime - startTime;
                    totalTimeForModelResolution += l;
                    System.out.println("[DEBUG] Resolved POM for " + groupId + ":" + artifactId + ":" + version + " in "
                            + l + " ms"); // TODO: remove
                    declaredDepsByModuleGAV.put(key,
                            effectiveModel);
                } catch (UnresolvableModelException | ModelBuildingException e) {
                    project.getLogger().warn("Unable to resolve effective model for {}:{}:{}: {}",
                            groupId, artifactId, version, e.getMessage());
                }

            } else if (component.getId() instanceof ProjectComponentIdentifier projectId) {
                Project depProject = project.findProject(projectId.getProjectPath());
                if (depProject == null) {
                    continue;
                }
                declaredDepsByProjectPath.putIfAbsent(depProject.getPath(),
                        // TODO: make sure we collect deps properly for application project
                        DependencyInfoCollector.collectDeclaredFromProject(depProject, depProject.equals(project)));
            }
        }

        System.out.println("[DEBUG] Total time for model resolution in configuration '"
                + configuration.getName() + "' of project '" + project.getPath() + "': "
                + totalTimeForModelResolution + " ms"); // TODO: remove
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
