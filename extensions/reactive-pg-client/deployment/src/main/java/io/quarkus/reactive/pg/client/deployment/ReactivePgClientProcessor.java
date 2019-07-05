package io.quarkus.reactive.pg.client.deployment;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.reactive.pg.client.runtime.DataSourceConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolProducer;
import io.quarkus.reactive.pg.client.runtime.PgPoolRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.reactiverse.pgclient.PgPool;

class ReactivePgClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(ReactivePgClientProcessor.class.getName());

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(PgPoolProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    PgPoolBuildItem build(BuildProducer<FeatureBuildItem> feature, PgPoolRecorder recorder, VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown,
            DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REACTIVE_PG_CLIENT));

        RuntimeValue<PgPool> pgPool = recorder.configurePgPool(vertx.getVertx(), beanContainer.getValue(), dataSourceConfig,
                pgPoolConfig, shutdown);

        return new PgPoolBuildItem(pgPool);
    }
}
