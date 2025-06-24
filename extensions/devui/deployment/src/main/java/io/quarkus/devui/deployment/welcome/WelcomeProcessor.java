package io.quarkus.devui.deployment.welcome;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devui.deployment.ExtensionsBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Welcome page
 */
public class WelcomeProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    InternalPageBuildItem createWelcomePages(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ExtensionsBuildItem extensionsBuildItem) {

        InternalPageBuildItem welcomePageBuildItem = new InternalPageBuildItem("Welcome", 99999);

        welcomePageBuildItem.addBuildTimeData("welcomeData", createWelcomeData(curateOutcomeBuildItem, extensionsBuildItem));

        welcomePageBuildItem.addPage(Page.webComponentPageBuilder()
                .namespace("devui-welcome")
                .title("Welcome")
                .icon("font-awesome-brands:redhat")
                .componentLink("qwc-welcome.js")
                .excludeFromMenu());

        return welcomePageBuildItem;
    }

    private WelcomeData createWelcomeData(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ExtensionsBuildItem extensionsBuildItem) {

        WorkspaceModule workspaceModule = curateOutcomeBuildItem.getApplicationModel().getApplicationModule();

        WelcomeData welcomeData = new WelcomeData();
        welcomeData.configFile = getConfigFile(workspaceModule);
        welcomeData.sourceDir = getSourceDir(workspaceModule);
        welcomeData.resourcesDir = getResourcesDir(workspaceModule);

        List<Extension> selectedExtensions = getSelectedExtensions(workspaceModule, extensionsBuildItem);
        for (Extension extension : selectedExtensions) {
            if (extension != null && extension.getName() != null) {
                welcomeData.addSelectedExtension(extension.getName(), extension.getDescription(), extension.getGuide());
            }
        }

        return welcomeData;
    }

    private String getConfigFile(WorkspaceModule workspaceModule) {
        if (workspaceModule != null) {
            File moduleDir = workspaceModule.getModuleDir();
            if (moduleDir != null) {
                String root = moduleDir.toPath().toString();
                Collection<SourceDir> resourcesDirs = workspaceModule.getMainSources().getResourceDirs();
                if (resourcesDirs != null && !resourcesDirs.isEmpty()) {
                    Path resourceDirs = resourcesDirs.iterator().next().getDir();
                    Path propertiesFile = resourceDirs.resolve("application.properties");
                    if (Files.exists(propertiesFile)) {
                        return propertiesFile.toString().replace("\\", "/").substring((int) root.length() + 1);
                    }
                    Path propertiesYaml = resourceDirs.resolve("application.yaml");
                    if (Files.exists(propertiesYaml)) {
                        return propertiesYaml.toString().replace("\\", "/").substring((int) root.length() + 1);
                    }
                }
            }
        }
        return null;
    }

    private String getSourceDir(WorkspaceModule workspaceModule) {
        if (workspaceModule != null) {
            File moduleDir = workspaceModule.getModuleDir();
            if (moduleDir != null) {
                String root = moduleDir.toPath().toString();
                Collection<SourceDir> sourceDirs = workspaceModule.getMainSources().getSourceDirs();
                if (sourceDirs != null && !sourceDirs.isEmpty()) {
                    String sourceDir = sourceDirs.iterator().next().getDir().toString().replace("\\", "/");
                    return sourceDir.substring((int) root.length() + 1);
                }
            }
        }
        return null;
    }

    private String getResourcesDir(WorkspaceModule workspaceModule) {
        if (workspaceModule != null) {
            File moduleDir = workspaceModule.getModuleDir();
            if (moduleDir != null) {
                String root = moduleDir.toPath().toString();
                Collection<SourceDir> resourcesDirs = workspaceModule.getMainSources().getResourceDirs();
                if (resourcesDirs != null && !resourcesDirs.isEmpty()) {
                    String resourceDirs = resourcesDirs.iterator().next().getDir().toString().replace("\\", "/");
                    return resourceDirs.substring((int) root.length() + 1);
                }
            }
        }
        return null;
    }

    private List<Extension> getSelectedExtensions(WorkspaceModule workspaceModule,
            ExtensionsBuildItem extensionsBuildItem) {

        Map<String, Extension> extensionMap = getExtensionMap(extensionsBuildItem);

        if (workspaceModule != null) {
            List<Extension> selectedDependency = workspaceModule.getDirectDependencies()
                    .stream()
                    .filter((dependency) -> {
                        return dependency.isJar()
                                && (dependency.getScope() == null || !dependency.getScope().equals("test"))
                                && !dependency.getGroupId().startsWith("org.mvnpm")
                                && !dependency.getGroupId().startsWith("org.webjars");
                    }).map((t) -> {
                        String key = t.getGroupId() + ":" + t.getArtifactId();
                        return extensionMap.get(key);
                    }).collect(Collectors.toList());
            return selectedDependency;
        } else {
            return List.of();
        }
    }

    private Map<String, Extension> getExtensionMap(ExtensionsBuildItem extensionsBuildItem) {
        Map<String, Extension> all = new HashMap<>();
        extensionsBuildItem.getActiveExtensions().forEach((t) -> {
            String gav[] = t.getArtifact().split(":");
            all.put(gav[0] + ":" + gav[1], t);
        });
        extensionsBuildItem.getInactiveExtensions().forEach((t) -> {
            String gav[] = t.getArtifact().split(":");
            all.put(gav[0] + ":" + gav[1], t);
        });
        return all;
    }

}
