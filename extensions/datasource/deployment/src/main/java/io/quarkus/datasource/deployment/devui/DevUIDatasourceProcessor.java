package io.quarkus.datasource.deployment.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.devui.DatasourceJsonRpcService;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevUIDatasourceProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(DataSourcesBuildTimeConfig dataSourceBuildTimeConfig) {
        CardPageBuildItem card = new CardPageBuildItem();

        List<String> names = new ArrayList<>();
        names.add("<default>");
        names.addAll(dataSourceBuildTimeConfig.dataSources().keySet());
        Collections.sort(names);
        card.addBuildTimeData("datasources", names);

        card.addPage(Page.webComponentPageBuilder().title("Reset")
                .componentLink("qwc-datasources-reset.js")
                .icon("font-awesome-solid:broom"));
        return card;
    }

    @BuildStep
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(DatasourceJsonRpcService.class);
    }

}
