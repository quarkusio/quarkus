package io.quarkus.agroal.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AgroalRecorder {

    public static final String DEFAULT_DATASOURCE_NAME = "<default>";

    public BeanContainerListener addDataSource(
            Class<? extends AbstractDataSourceProducer> dataSourceProducerClass,
            AgroalBuildTimeConfig agroalBuildTimeConfig,
            boolean disableSslSupport) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                AbstractDataSourceProducer producer = beanContainer.instance(dataSourceProducerClass);

                producer.setBuildTimeConfig(agroalBuildTimeConfig);

                if (disableSslSupport) {
                    producer.disableSslSupport();
                }
            }
        };
    }

    public void configureRuntimeProperties(AgroalRuntimeConfig agroalRuntimeConfig) {
        Arc.container().instance(AbstractDataSourceProducer.class).get().setRuntimeConfig(agroalRuntimeConfig);
    }
}
