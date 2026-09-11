package io.quarkus.gradle.model.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.model.config.LocalComponentOutputViews;
import io.quarkus.gradle.model.config.StrictApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.model.pom.DeclaredDependencyEnrichmentMode;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;
import io.quarkus.runtime.LaunchMode;

class ApplicationModelTaskConfiguratorTest {

    @Test
    void strictConfiguratorWiresModelTaskWithoutLiveProjectDeclaredDependencyEnrichment() {
        Project project = configuredJavaProject();
        var classpath = new StrictApplicationDeploymentClasspathBuilder(project, LaunchMode.NORMAL, "quarkusApplication");
        GenerateApplicationModelTask task = project.getTasks()
                .create("quarkusApplicationModel", GenerateApplicationModelTask.class, LaunchMode.NORMAL);
        DefaultProjectDescriptor projectDescriptor = new DefaultProjectDescriptor(null);

        StrictApplicationModelTaskConfigurator.configureNoLiveProjectDeclaredDependencies(project, task,
                project.provider(() -> projectDescriptor), classpath, LaunchMode.NORMAL, false);

        assertThat(task.getProjectDescriptor().get()).isSameAs(projectDescriptor);
        assertThat(task.getLaunchMode().get()).isEqualTo(LaunchMode.NORMAL);
        assertThat(task.getTypeModel().get()).isEqualTo(":quarkusApplicationModel");
        assertThat(task.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.SELECTED_MODULE_POMS);
        assertThat(task.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-app-model.dat").get().getAsFile());
        assertThat(task.getOriginalClasspath()).containsExactlyInAnyOrderElementsOf(
                project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
        assertThat(task.getLocalClassOutputArtifacts().get()).isEmpty();
        assertThat(task.getLocalResourceOutputArtifacts().get()).isEmpty();
        assertThat(task.getLocalComponentOutputFiles()).isEmpty();
    }

    @Test
    void strictConfiguratorSupportsBuildModelOutputForNormalMode() {
        Project project = configuredJavaProject();
        var classpath = new StrictApplicationDeploymentClasspathBuilder(project, LaunchMode.NORMAL, "quarkusApplication");
        GenerateApplicationModelTask task = project.getTasks()
                .create("quarkusApplicationBuildModel", GenerateApplicationModelTask.class, LaunchMode.NORMAL);

        StrictApplicationModelTaskConfigurator.configureNoLiveProjectDeclaredDependencies(project, task,
                project.provider(() -> new DefaultProjectDescriptor(null)), classpath, LaunchMode.NORMAL, true);

        assertThat(task.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-app-model-build.dat").get().getAsFile());
    }

    @Test
    void isolatedConfiguratorRegistersTaskWithLocalOutputArtifactsAndNoDeclaredDependencyEnrichment() {
        Project project = configuredJavaProject();
        var classpath = new StrictApplicationDeploymentClasspathBuilder(project, LaunchMode.DEVELOPMENT, "quarkusApplication");
        var localOutputs = LocalComponentOutputViews.of(project.getObjects(),
                classpath.getRuntimeConfigurationWithoutResolvingDeployment());

        var taskProvider = IsolatedApplicationModelTaskConfigurator.registerGenerateApplicationModelTask(project,
                project.provider(() -> new DefaultProjectDescriptor(null)), classpath, LaunchMode.DEVELOPMENT, localOutputs);

        GenerateApplicationModelTask task = taskProvider.get();
        assertThat(task.getName()).isEqualTo("quarkusGenerateDevAppModel");
        assertThat(task.getLaunchMode().get()).isEqualTo(LaunchMode.DEVELOPMENT);
        assertThat(task.getDeclaredDependencyEnrichmentMode().get()).isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(task.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-app-dev-model.dat").get().getAsFile());
        assertThat(task.getLocalClassOutputArtifacts().get()).isEqualTo(Set.of());
        assertThat(task.getLocalResourceOutputArtifacts().get()).isEqualTo(Set.of());
        assertThat(task.getLocalComponentOutputFiles()).isEmpty();
    }

    private static Project configuredJavaProject() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(JavaPlugin.class);
        StrictApplicationDeploymentClasspathBuilder.initConfigurations(project);
        return project;
    }
}
