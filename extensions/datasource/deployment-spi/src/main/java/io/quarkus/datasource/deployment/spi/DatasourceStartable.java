package io.quarkus.datasource.deployment.spi;

import io.quarkus.deployment.builditem.Startable;

public interface DatasourceStartable extends Startable {
    DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevServicesDatasource();

}
