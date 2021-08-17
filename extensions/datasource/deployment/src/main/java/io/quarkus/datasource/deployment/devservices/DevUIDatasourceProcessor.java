package io.quarkus.datasource.deployment.devservices;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DatabaseRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class DevUIDatasourceProcessor {

    @BuildStep
    public DevConsoleTemplateInfoBuildItem devConsoleInfo(
            DataSourcesBuildTimeConfig dataSourceBuildTimeConfig) {
        List<String> names = new ArrayList<>();
        names.add("<default>");
        names.addAll(dataSourceBuildTimeConfig.namedDataSources.keySet());
        Collections.sort(names);
        return new DevConsoleTemplateInfoBuildItem("dbs", names);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem devConsoleCleanDatabaseHandler(DatabaseRecorder recorder) {
        return new DevConsoleRouteBuildItem("reset", "POST", recorder.devConsoleResetDatabaseHandler());
    }
}
