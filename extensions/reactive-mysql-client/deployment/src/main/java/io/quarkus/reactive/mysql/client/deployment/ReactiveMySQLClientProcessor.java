package io.quarkus.reactive.mysql.client.deployment;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.reactive.mysql.client.runtime.DataSourceConfig;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolConfig;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolProducer;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mysqlclient.MySQLPool;

class ReactiveMySQLClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(ReactiveMySQLClientProcessor.class.getName());

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(MySQLPoolProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    MySQLPoolBuildItem build(BuildProducer<FeatureBuildItem> feature, MySQLPoolRecorder recorder, VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown,
            DataSourceConfig dataSourceConfig, MySQLPoolConfig mysqlPoolConfig) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REACTIVE_MYSQL_CLIENT));

        RuntimeValue<MySQLPool> mysqlPool = recorder.configureMySQLPool(vertx.getVertx(), beanContainer.getValue(),
                dataSourceConfig,
                mysqlPoolConfig, shutdown);

        return new MySQLPoolBuildItem(mysqlPool);
    }
}
