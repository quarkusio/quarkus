package io.quarkus.datasource.deployment;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;

public class DataSourceBindingProcessor {

    private static final String DEFAULT_DATASOURCE = "default";

    @BuildStep
    public void process(DataSourcesBuildTimeConfig config, BuildProducer<ServiceBindingQualifierBuildItem> bindings) {
        config.dataSources().forEach((name, c) -> {
            c.dbKind().ifPresent(dbKind -> {
                if (DataSourceUtil.isDefault(name)) {
                    bindings.produce(new ServiceBindingQualifierBuildItem(dbKind, dbKind, DEFAULT_DATASOURCE));
                } else {
                    bindings.produce(new ServiceBindingQualifierBuildItem(dbKind, name));
                }
            });
        });
    }
}
