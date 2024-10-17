package io.quarkus.devui.deployment.menu;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.deployment.IsDevelopment;
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
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
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

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createExtensionsPages(ExtensionsBuildItem extensionsBuildItem) {

        InternalPageBuildItem extensionsPages = new InternalPageBuildItem("Extensions", 10);

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

        return extensionsPages;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void createBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            LaunchModeBuildItem launchModeBuildItem) {

        if (launchModeBuildItem.getDevModeType().isPresent()
                && launchModeBuildItem.getDevModeType().get().equals(DevModeType.LOCAL)) {

            Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            QuarkusProject quarkusProject = QuarkusProjectHelper.getProject(projectRoot);

            BuildTimeActionBuildItem buildTimeActions = new BuildTimeActionBuildItem(NAMESPACE);

            getCategories(buildTimeActions, quarkusProject);
            getInstallableExtensions(buildTimeActions, quarkusProject);
            getInstalledNamespaces(buildTimeActions, quarkusProject);
            removeExtension(buildTimeActions, quarkusProject);
            addExtension(buildTimeActions, quarkusProject);
            buildTimeActionProducer.produce(buildTimeActions);
        }
    }

    private void getCategories(BuildTimeActionBuildItem buildTimeActions, QuarkusProject quarkusProject) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), ignored -> {

            try {
                QuarkusCommandOutcome outcome = new ListCategories(quarkusProject)
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
    }

    private void getInstallableExtensions(BuildTimeActionBuildItem buildTimeActions, QuarkusProject quarkusProject) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), ignored -> {

            try {
                QuarkusCommandOutcome outcome = new ListExtensions(quarkusProject)
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
    }

    private void getInstalledNamespaces(BuildTimeActionBuildItem buildTimeActions, QuarkusProject quarkusProject) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), ignored -> {

            try {

                QuarkusCommandOutcome outcome = new ListExtensions(quarkusProject)
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
            } catch (QuarkusCommandException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void removeExtension(BuildTimeActionBuildItem buildTimeActions, QuarkusProject quarkusProject) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), params -> {
            String extensionArtifactId = params.get("extensionArtifactId");

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    PrintStream printStream = new PrintStream(byteArrayOutputStream)) {

                QuarkusCommandOutcome outcome = new RemoveExtensions(quarkusProject, MessageWriter.info(printStream))
                        .extensions(Set.of(extensionArtifactId))
                        .execute();

                if (outcome.isSuccess()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void addExtension(BuildTimeActionBuildItem buildTimeActions, QuarkusProject quarkusProject) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(), params -> {
            String extensionArtifactId = params.get("extensionArtifactId");

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    PrintStream printStream = new PrintStream(byteArrayOutputStream)) {

                QuarkusCommandOutcome outcome = new AddExtensions(quarkusProject, MessageWriter.info(printStream))
                        .extensions(Set.of(extensionArtifactId))
                        .execute();

                if (outcome.isSuccess()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static final String NAMESPACE = "devui-extensions";
}
