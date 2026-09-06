package io.quarkus.datasource.deployment.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.runtime.dev.ui.DatasourceJsonRpcService;
import io.quarkus.datasource.runtime.produi.DataSourceProdUIService;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

public class DevUIDatasourceProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem create(List<DataSourceDefinedBuildItem> definedDataSources) {
        CardPageBuildItem card = new CardPageBuildItem();

        List<String> names = definedDataSources.stream().map(DataSourceDefinedBuildItem::getName)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.sort(names);
        card.addBuildTimeData("datasources", names);

        card.addPage(Page.webComponentPageBuilder().title("Reset")
                .componentLink("qwc-datasources-reset.js")
                .icon("font-awesome-solid:broom"));
        return card;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(DatasourceJsonRpcService.class);
    }

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(DataSourceProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Datasources")
                .icon("font-awesome-solid:database")
                .componentLink("pwc-datasource-list.js"));
        return page;
    }

}
