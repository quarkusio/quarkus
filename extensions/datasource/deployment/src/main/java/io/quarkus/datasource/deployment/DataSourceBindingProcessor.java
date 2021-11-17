
package io.quarkus.agroal.deployment;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceQualifierBuildItem;

public class DataSourceBindingProcessor {

    private static final String DB_KIND = "DB_KIND";

    @BuildStep
    public void process(DataSourcesBuildTimeConfig config, BuildProducer<ServiceQualifierBuildItem> bindings) {
        config.defaultDataSource.dbKind.ifPresent(k -> {
            bindings.produce(createBuildItem(k, k));
        });

        config.namedDataSources.forEach((n, c) -> {
            c.dbKind.ifPresent(dbKind -> {
                bindings.produce(createBuildItem(dbKind, n));
            });
        });
    }

    private ServiceQualifierBuildItem createBuildItem(String dbKind, String name) {
        return new ServiceQualifierBuildItem(dbKind, name);
    }
}
