package io.quarkus.caffeine.deployment.devui;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;

/**
 * Dev UI card for displaying important details such as the Caffeine library version.
 */
public class CaffeineDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();

        final PageBuilder versionPage = Page.externalPageBuilder("Version")
                .icon("font-awesome-solid:mug-hot")
                .url("https://github.com/ben-manes/caffeine")
                .doNotEmbed()
                .staticLabel(getManifest(Caffeine.class).getMainAttributes().getValue("Bundle-Version"));
        card.addPage(versionPage);

        card.setCustomCard("qwc-caffeine-card.js");

        cardPageBuildItemBuildProducer.produce(card);
    }

    public static Manifest getManifest(Class<?> clz) {
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
