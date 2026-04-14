package io.quarkus.webdependency.locator.deployment.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devshell.spi.ShellPageBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.welcome.DynamicWelcomeBuildItem;
import io.quarkus.webdependency.locator.deployment.ImportMapBuildItem;
import io.quarkus.webdependency.locator.runtime.dev.shell.WebDependencyLocatorShellPage;

public class WebDependencyLocatorDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer,
            List<WebDependencyLibrariesBuildItem> webDependencyLibrariesBuildItems,
            Optional<ImportMapBuildItem> importMapBuildItem) {

        List<WebDependencyLibrary> webDependencyLibraries = new ArrayList<>();
        for (WebDependencyLibrariesBuildItem webDependencyLibrariesBuildItem : webDependencyLibrariesBuildItems) {
            webDependencyLibraries.addAll(webDependencyLibrariesBuildItem.getWebDependencyLibraries());
        }

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        cardPageBuildItem.setLogo("javascript_logo.svg", "javascript_logo.svg");
        if (!webDependencyLibraries.isEmpty()) {
            // Web Dependency Libraries
            cardPageBuildItem.addBuildTimeData("webDependencyLibraries", webDependencyLibraries);

            // Web Dependency Asset List
            cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                    .componentLink("qwc-web-dependency-locator-libraries.js")
                    .title("Web libraries")
                    .icon("font-awesome-solid:folder-tree")
                    .staticLabel(String.valueOf(webDependencyLibraries.size())));

            if (importMapBuildItem.isPresent()) {
                cardPageBuildItem.addBuildTimeData("importMap", importMapBuildItem.get().getImportMap(),
                        "An importmap that maps all available JavaScript libraries that can be used in the application");

                // ImportMap
                cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                        .componentLink("qwc-web-dependency-locator-importmap.js")
                        .title("Import Map")
                        .icon("font-awesome-solid:diagram-project"));

            }

        }

        cardPageProducer.produce(cardPageBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DynamicWelcomeBuildItem createDynamicWelcomeData() {
        return new DynamicWelcomeBuildItem(DYNAMIC_WELCOME);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public BuildTimeActionBuildItem createBuildTimeActions(
            List<WebDependencyLibrariesBuildItem> webDependencyLibrariesBuildItems) {

        List<WebDependencyLibrary> webDependencyLibraries = new ArrayList<>();
        for (WebDependencyLibrariesBuildItem webDependencyLibrariesBuildItem : webDependencyLibrariesBuildItems) {
            webDependencyLibraries.addAll(webDependencyLibrariesBuildItem.getWebDependencyLibraries());
        }

        return new BuildTimeActionBuildItem().actionBuilder()
                .methodName("getWebDependencyLibraries")
                .function(params -> webDependencyLibraries)
                .build();
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public ShellPageBuildItem createShellPage() {
        return ShellPageBuildItem.withCustomPage("Web Libs", 'w',
                WebDependencyLocatorShellPage.class);
    }

    private static final String DYNAMIC_WELCOME = """
                <span>Learn how you can <a href="https://quarkus.io/guides/web-dependency-locator" target="_blank">add your own web content</a></span>
                <span>Static assets: <code>${devuiState.welcomeData.resourcesDir}/META-INF/resources/</code> OR </span>
                <span>Dynamic assets: <code>${devuiState.welcomeData.resourcesDir}/web</code></span>
            """;

}
