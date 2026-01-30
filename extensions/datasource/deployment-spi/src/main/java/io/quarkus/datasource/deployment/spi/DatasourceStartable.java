package io.quarkus.datasource.deployment.spi;

import io.quarkus.deployment.builditem.Startable;

public interface DatasourceStartable extends Startable {

    /**
     * Gets a record which can be used to interrogate the datasource, after it has been started.
     * This should not be called before the container has been started.
     * If the datasource has not been started, an IllegalStateException will be thrown.
     *
     * @return a RunningDevServicesDatasource, populated with the container id and urls and credentials of the running
     *         datasource
     */
    default DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevServicesDatasource() {
        if (getContainerId() == null) {
            throw new IllegalStateException(
                    "Cannot create a RunningDevServicesDatasource before the datasource has been started.");
        }
        // It would be nice to cache this, but since this is an interface, it can't be done at this level, and would have to be done by every implementing class
        return new DevServicesDatasourceProvider.RunningDevServicesDatasource(getContainerId(), getEffectiveJdbcUrl(),
                getReactiveUrl(), getUsername(), getPassword());
    }

    String getPassword();

    String getUsername();

    String getReactiveUrl();

    String getEffectiveJdbcUrl();

}
