package io.quarkus.infinispan.client.deployment.devui;

import org.infinispan.commons.util.Version;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.infinispan.client.runtime.devui.InfinispanClientsContainer;
import io.quarkus.infinispan.client.runtime.devui.InfinispanJsonRPCService;

public class InfinispanDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem infinispanServer() {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        final PageBuilder consoleLink = Page.externalPageBuilder("Infinispan Server Console")
                .dynamicUrlJsonRPCMethodName("getConsoleDefaultLink")
                .doNotEmbed()
                .icon("font-awesome-solid:server")
                .staticLabel(Version.getMajorMinor());

        cardPageBuildItem.addPage(consoleLink);

        final PageBuilder documentation = Page.externalPageBuilder("Documentation")
                .icon("font-awesome-solid:info")
                .url("https://infinispan.org/")
                .doNotEmbed();
        cardPageBuildItem.addPage(documentation);

        final PageBuilder codeTutorials = Page.externalPageBuilder("Code Tutorials")
                .icon("font-awesome-solid:hat-wizard")
                .url("https://github.com/infinispan/infinispan-simple-tutorials")
                .doNotEmbed();

        cardPageBuildItem.addPage(codeTutorials);

        return cardPageBuildItem;
    }

    @BuildStep
    public JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(InfinispanJsonRPCService.class, BuiltinScope.SINGLETON.getName());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(InfinispanClientsContainer.class);
    }
}
