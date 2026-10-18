package io.quarkus.datasource.deployment;

import java.util.List;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;

public class DataSourceBindingProcessor {

    private static final String DEFAULT_DATASOURCE = "default";

    @BuildStep
    public void process(List<DataSourceDefinedBuildItem> definedDataSources,
            BuildProducer<ServiceBindingQualifierBuildItem> bindings) {
        for (DataSourceDefinedBuildItem ds : definedDataSources) {
            String name = ds.getName();
            String dbKind = ds.getDbKind();
            if (DataSourceUtil.isDefault(name)) {
                bindings.produce(new ServiceBindingQualifierBuildItem(dbKind, dbKind, DEFAULT_DATASOURCE));
            } else {
                bindings.produce(new ServiceBindingQualifierBuildItem(dbKind, name));
            }
        }
    }
}
