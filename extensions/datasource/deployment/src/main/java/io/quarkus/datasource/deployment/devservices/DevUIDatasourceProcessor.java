package io.quarkus.datasource.deployment.devservices;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DatabaseRecorder;
import io.quarkus.datasource.runtime.DatasourceJsonRpcService;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevUIDatasourceProcessor {

    @BuildStep
    public DevConsoleTemplateInfoBuildItem devConsoleInfo(
            DataSourcesBuildTimeConfig dataSourceBuildTimeConfig) {
        List<String> names = new ArrayList<>();
        names.add("<default>");
        names.addAll(dataSourceBuildTimeConfig.namedDataSources().keySet());
        Collections.sort(names);
        return new DevConsoleTemplateInfoBuildItem("dbs", names);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem devConsoleCleanDatabaseHandler(DatabaseRecorder recorder) {
        return new DevConsoleRouteBuildItem("reset", "POST", recorder.devConsoleResetDatabaseHandler());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(DataSourcesBuildTimeConfig dataSourceBuildTimeConfig) {
        CardPageBuildItem card = new CardPageBuildItem();

        List<String> names = new ArrayList<>();
        names.add("<default>");
        names.addAll(dataSourceBuildTimeConfig.namedDataSources().keySet());
        Collections.sort(names);
        card.addBuildTimeData("datasources", names);

        card.addPage(Page.webComponentPageBuilder().title("Reset")
                .componentLink("qwc-datasources-reset.js")
                .icon("font-awesome-solid:broom"));
        return card;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(DatasourceJsonRpcService.class);
    }

}
