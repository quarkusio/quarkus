package io.quarkus.devui.deployment.menu;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.ListCategories;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.RemoveExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devui.deployment.DevUIConfig;
import io.quarkus.devui.deployment.ExtensionsBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.deployment.extension.ExtensionGroup;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Extensions Page
 */
public class ExtensionsProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    InternalPageBuildItem createExtensionsPages(ExtensionsBuildItem extensionsBuildItem, DevUIConfig config) {

        InternalPageBuildItem extensionsPages = new InternalPageBuildItem("Extensions", 10, "qwc-extensions-menu-action");

        // Extensions
        Map<ExtensionGroup, List<Extension>> response = Map.of(
                ExtensionGroup.active, extensionsBuildItem.getActiveExtensions(),
                ExtensionGroup.inactive, extensionsBuildItem.getInactiveExtensions());

        extensionsPages.addBuildTimeData("extensions", response);

        // Page
        extensionsPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Extensions")
                .icon("font-awesome-solid:puzzle-piece")
                .componentLink("qwc-extensions.js"));

        extensionsPages.addBuildTimeData("allowExtensionManagement", config.allowExtensionManagement());

        return extensionsPages;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            LaunchModeBuildItem launchModeBuildItem, DevUIConfig config) {

        if (launchModeBuildItem.getDevModeType().isPresent()
                && launchModeBuildItem.getDevModeType().get().equals(DevModeType.LOCAL)
                && config.allowExtensionManagement()) {

            BuildTimeActionBuildItem buildTimeActions = new BuildTimeActionBuildItem(NAMESPACE);
            getCategories(buildTimeActions);
            getInstallableExtensions(buildTimeActions);
            getInstalledNamespaces(buildTimeActions);
            removeExtension(buildTimeActions);
            addExtension(buildTimeActions);
            buildTimeActionProducer.produce(buildTimeActions);
        }
    }

    private void getCategories(BuildTimeActionBuildItem buildTimeActions) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), ignored -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    QuarkusCommandOutcome outcome = new ListCategories(getQuarkusProject())
                            .format("object")
                            .execute();

                    if (outcome.isSuccess()) {
                        return outcome.getResult();
                    }
                } catch (QuarkusCommandException ex) {
                    throw new RuntimeException(ex);
                }
                return null;
            });
        });
    }

    private void getInstallableExtensions(BuildTimeActionBuildItem buildTimeActions) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), ignored -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    QuarkusCommandOutcome outcome = new ListExtensions(getQuarkusProject())
                            .installed(false)
                            .all(false)
                            .format("object")
                            .execute();

                    if (outcome.isSuccess()) {
                        return outcome.getResult();
                    }

                    return null;
                } catch (QuarkusCommandException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void getInstalledNamespaces(BuildTimeActionBuildItem buildTimeActions) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), ignored -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    QuarkusCommandOutcome outcome = new ListExtensions(getQuarkusProject())
                            .installed(true)
                            .all(false)
                            .format("object")
                            .execute();

                    if (outcome.isSuccess()) {

                        List<io.quarkus.registry.catalog.Extension> extensionList = (List<io.quarkus.registry.catalog.Extension>) outcome
                                .getResult();

                        List<String> namespaceList = new ArrayList<>();

                        if (!extensionList.isEmpty()) {
                            for (io.quarkus.registry.catalog.Extension e : extensionList) {
                                String groupId = e.getArtifact().getGroupId();
                                String artifactId = e.getArtifact().getArtifactId();
                                namespaceList.add(groupId + "." + artifactId);
                            }
                        }
                        return namespaceList;
                    }

                    return null;
                } catch (IllegalStateException e) {
                    return null;
                } catch (QuarkusCommandException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void removeExtension(BuildTimeActionBuildItem buildTimeActions) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), params -> {
            return CompletableFuture.supplyAsync(() -> {
                String extensionArtifactId = params.get("extensionArtifactId");
                try {
                    QuarkusCommandOutcome outcome = new RemoveExtensions(getQuarkusProject())
                            .extensions(Set.of(extensionArtifactId))
                            .execute();

                    if (outcome.isSuccess()) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (QuarkusCommandException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void addExtension(BuildTimeActionBuildItem buildTimeActions) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), params -> {
            return CompletableFuture.supplyAsync(() -> {
                String extensionArtifactId = params.get("extensionArtifactId");

                try {
                    QuarkusCommandOutcome outcome = new AddExtensions(getQuarkusProject())
                            .extensions(Set.of(extensionArtifactId))
                            .execute();

                    if (outcome.isSuccess()) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (QuarkusCommandException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private QuarkusProject getQuarkusProject() {
        Path projectRoot;
        String gradlePath = System.getProperty("gradle.project.path");
        if (gradlePath != null) {
            projectRoot = Path.of(gradlePath);
        } else {
            projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
        return QuarkusProjectHelper.getCachedProject(projectRoot);
    }

    private static final String NAMESPACE = "devui-extensions";
}
