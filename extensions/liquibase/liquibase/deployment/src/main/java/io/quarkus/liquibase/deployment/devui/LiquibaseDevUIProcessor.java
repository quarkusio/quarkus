package io.quarkus.liquibase.deployment.devui;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.liquibase.runtime.dev.ui.LiquibaseJsonRpcService;
import liquibase.changelog.DatabaseChangeLog;

/**
 * Dev UI card for displaying important details such as the library version.
 */
public class LiquibaseDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();

        // card
        card.setCustomCard("qwc-liquibase-card.js");

        // pages
        card.addPage(Page.externalPageBuilder("Version")
                .icon("font-awesome-solid:book")
                .url("https://www.liquibase.org/")
                .doNotEmbed()
                .staticLabel(getManifest(DatabaseChangeLog.class).getMainAttributes().getValue("Bundle-Version")));

        card.addPage(Page.webComponentPageBuilder().title("Datasources")
                .componentLink("qwc-liquibase-datasources.js")
                .icon("font-awesome-solid:database")
                .dynamicLabelJsonRPCMethodName("getDatasourceCount"));

        cardPageBuildItemBuildProducer.produce(card);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(LiquibaseJsonRpcService.class);
    }

    private static Manifest getManifest(Class<?> clz) {
        String resource = "/" + clz.getName().replace(".", "/") + ".class";
        String fullPath = clz.getResource(resource).toString();
        String archivePath = fullPath.substring(0, fullPath.length() - resource.length());

        try (InputStream input = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream()) {
            return new Manifest(input);
        } catch (Exception e) {
            throw new RuntimeException("Loading MANIFEST for class " + clz + " failed!", e);
        }
    }
}
