package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.tasks.QuarkusApplicationDeployTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageReferenceResolutionTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageReferenceResolutionTask;

class QuarkusApplicationImageDeploymentRegistrationTest {

    @Test
    void wiresImageAotAndDeploymentReceiptsWithoutExecutingExternalWork() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> builds.aotJar("app", QuarkusApplicationJvmStartupArchiveType.AOT, app -> {
            app.image(image -> {
                image.getRepository().set("example/app");
                image.getBuilder().set(QuarkusApplicationImageBuilder.DOCKER);
            });
            app.startupArchive(archive -> archive.getFile()
                    .set(project.getLayout().getProjectDirectory().file("build/aot/app.aot")));
            app.startupOptimizedImage(ignored -> {
            });
            app.deployments(deployments -> {
                deployments.kubernetes("dev");
                deployments.openshift("prod",
                        deployment -> deployment.getImageSource()
                                .set(QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH));
            });
        }));

        QuarkusApplicationImageBuildTask imageBuild = (QuarkusApplicationImageBuildTask) project.getTasks()
                .getByName("quarkusAppImageBuild");
        QuarkusApplicationImageReferenceResolutionTask imageBuildPreflight = (QuarkusApplicationImageReferenceResolutionTask) project
                .getTasks().getByName("quarkusAppImageBuildReferencePreflight");
        assertThat(imageBuild.getTaskDependencies().getDependencies(imageBuild))
                .extracting(Task::getName)
                .contains(imageBuildPreflight.getName());
        assertThat(imageBuild.getImageReferencePreflightReceiptFile().get().getAsFile())
                .isEqualTo(imageBuildPreflight.getResolutionReceiptFile().get().getAsFile());
        assertThat(imageBuild.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/image-build/image-build-result.properties").get().getAsFile());

        QuarkusApplicationImagePushTask imagePush = (QuarkusApplicationImagePushTask) project.getTasks()
                .getByName("quarkusAppImagePush");
        QuarkusApplicationImageReferenceResolutionTask imagePushPreflight = (QuarkusApplicationImageReferenceResolutionTask) project
                .getTasks().getByName("quarkusAppImagePushReferencePreflight");
        assertThat(imagePush.getTaskDependencies().getDependencies(imagePush))
                .extracting(Task::getName)
                .contains(imagePushPreflight.getName());
        assertThat(imagePush.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/image-push/image-push-result.properties").get().getAsFile());

        QuarkusApplicationStartupOptimizedImageBuildTask optimizedBuild = (QuarkusApplicationStartupOptimizedImageBuildTask) project
                .getTasks().getByName("quarkusAppStartupOptimizedImageBuild");
        QuarkusApplicationStartupOptimizedImageReferenceResolutionTask optimizedBuildPreflight = (QuarkusApplicationStartupOptimizedImageReferenceResolutionTask) project
                .getTasks().getByName("quarkusAppStartupOptimizedImageBuildReferencePreflight");
        assertThat(optimizedBuildPreflight.getTaskDependencies().getDependencies(optimizedBuildPreflight))
                .extracting(Task::getName)
                .contains(imageBuildPreflight.getName());
        assertThat(optimizedBuild.getTaskDependencies().getDependencies(optimizedBuild))
                .extracting(Task::getName)
                .contains(optimizedBuildPreflight.getName());
        assertThat(optimizedBuild.getBaseImageReceiptFile().get().getAsFile())
                .isEqualTo(imageBuild.getReceiptFile().get().getAsFile());
        assertThat(optimizedBuild.getOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .dir("quarkus-builds/app/startup-optimized-image-build").get().getAsFile());
        assertThat(optimizedBuild.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/startup-optimized-image-build/"
                                + "startup-optimized-image-build-result.properties")
                        .get().getAsFile());
        assertThat(optimizedBuild.getArchiveType().get()).isEqualTo(QuarkusApplicationJvmStartupArchiveType.AOT);
        assertThat(optimizedBuild.getArchiveFile().get().getAsFile())
                .isEqualTo(project.file("build/aot/app.aot"));
        assertThat(optimizedBuild.getArchiveDirectory().isPresent()).isFalse();
        assertThat(optimizedBuild.getImageSuffix().get()).isEqualTo("-aot");

        QuarkusApplicationStartupOptimizedImagePushTask optimizedPush = (QuarkusApplicationStartupOptimizedImagePushTask) project
                .getTasks().getByName("quarkusAppStartupOptimizedImagePush");
        QuarkusApplicationStartupOptimizedImageReferenceResolutionTask optimizedPushPreflight = (QuarkusApplicationStartupOptimizedImageReferenceResolutionTask) project
                .getTasks().getByName("quarkusAppStartupOptimizedImagePushReferencePreflight");
        assertThat(optimizedPushPreflight.getTaskDependencies().getDependencies(optimizedPushPreflight))
                .extracting(Task::getName)
                .contains(imageBuildPreflight.getName());
        assertThat(optimizedPush.getTaskDependencies().getDependencies(optimizedPush))
                .extracting(Task::getName)
                .contains(optimizedPushPreflight.getName());
        assertThat(optimizedPush.getBaseImageReceiptFile().get().getAsFile())
                .isEqualTo(imageBuild.getReceiptFile().get().getAsFile());
        assertThat(optimizedPush.getTaskDependencies().getDependencies(optimizedPush))
                .extracting(Task::getName)
                .contains("quarkusAppImageBuild")
                .doesNotContain("quarkusAppImagePush");
        assertThat(optimizedPush.getOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .dir("quarkus-builds/app/startup-optimized-image-push").get().getAsFile());
        assertThat(optimizedPush.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/startup-optimized-image-push/"
                                + "startup-optimized-image-push-result.properties")
                        .get().getAsFile());

        QuarkusApplicationDeployTask devDeploy = (QuarkusApplicationDeployTask) project.getTasks()
                .getByName("quarkusAppDeployToDev");
        assertThat(devDeploy.getDeploymentTarget().get()).isEqualTo(QuarkusApplicationDeploymentTarget.KUBERNETES);
        assertThat(devDeploy.getImageSource().get()).isEqualTo(QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH);
        assertThat(devDeploy.getNormalImagePushReceiptFile().get().getAsFile())
                .isEqualTo(imagePush.getReceiptFile().get().getAsFile());
        assertThat(devDeploy.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/deployments/dev/deployment-result.properties").get().getAsFile());

        QuarkusApplicationDeployTask prodDeploy = (QuarkusApplicationDeployTask) project.getTasks()
                .getByName("quarkusAppDeployToProd");
        assertThat(prodDeploy.getDeploymentTarget().get()).isEqualTo(QuarkusApplicationDeploymentTarget.OPENSHIFT);
        assertThat(prodDeploy.getImageSource().get())
                .isEqualTo(QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH);
        assertThat(prodDeploy.getStartupOptimizedImagePushReceiptFile().get().getAsFile())
                .isEqualTo(optimizedPush.getReceiptFile().get().getAsFile());
        assertThat(prodDeploy.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/deployments/prod/deployment-result.properties").get().getAsFile());
    }

    @Test
    void registersImageTasksWithoutImageConfiguration() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> builds.fastJar("app"));

        QuarkusApplicationImageBuildTask imageBuild = (QuarkusApplicationImageBuildTask) project.getTasks()
                .getByName("quarkusAppImageBuild");
        assertThat(imageBuild.getImageReference().isPresent()).isFalse();
        assertThat(imageBuild.getImageRepository().isPresent()).isFalse();
        assertThat(imageBuild.getImageTag().isPresent()).isFalse();
        assertThat(imageBuild.getImageBuilder().isPresent()).isFalse();
        assertThat(imageBuild.getTaskDependencies().getDependencies(imageBuild))
                .extracting(Task::getName)
                .doesNotContain("quarkusAppBuild");

        QuarkusApplicationImagePushTask imagePush = (QuarkusApplicationImagePushTask) project.getTasks()
                .getByName("quarkusAppImagePush");
        assertThat(imagePush.getImageReference().isPresent()).isFalse();
        assertThat(imagePush.getImageRepository().isPresent()).isFalse();
        assertThat(imagePush.getImageTag().isPresent()).isFalse();
        assertThat(imagePush.getImageBuilder().isPresent()).isFalse();
        assertThat(imagePush.getTaskDependencies().getDependencies(imagePush))
                .extracting(Task::getName)
                .doesNotContain("quarkusAppBuild");
    }
}
