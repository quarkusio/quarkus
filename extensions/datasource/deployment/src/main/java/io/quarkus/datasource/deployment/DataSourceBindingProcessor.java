package io.quarkus.datasource.deployment;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;

public class DataSourceBindingProcessor {

    @BuildStep
    public void process(DataSourcesBuildTimeConfig config, BuildProducer<ServiceBindingQualifierBuildItem> bindings) {
        config.dataSources().forEach((n, c) -> {
            c.dbKind().ifPresent(dbKind -> {
                bindings.produce(new ServiceBindingQualifierBuildItem(dbKind, n));
            });
        });
    }
}
