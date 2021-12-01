
package io.quarkus.agroal.deployment;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;

public class DataSourceBindingProcessor {

    private static final String DB_KIND = "DB_KIND";
    private static final String DEFAULT_DATASOURCE = "default";

    @BuildStep
    public void process(DataSourcesBuildTimeConfig config, BuildProducer<ServiceBindingQualifierBuildItem> bindings) {
        config.defaultDataSource.dbKind.ifPresent(k -> {
            bindings.produce(new ServiceBindingQualifierBuildItem(k, k, DEFAULT_DATASOURCE));
        });

        config.namedDataSources.forEach((n, c) -> {
            c.dbKind.ifPresent(dbKind -> {
                bindings.produce(new ServiceBindingQualifierBuildItem(dbKind, n));
            });
        });
    }
}
