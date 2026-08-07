package io.quarkus.devservices.datasource.common;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.devservices.common.StartableContainer;

/**
 * A {@link StartableContainer} that also implements {@link DatasourceStartable},
 * delegating datasource-specific methods to the wrapped container.
 * <p>
 * This allows datasource dev service containers to benefit from the shared
 * {@link StartableContainer} behavior (such as {@link #isReusable()}) while
 * still exposing the {@link DatasourceStartable} interface needed by the
 * datasource dev services infrastructure.
 *
 * @param <T> the concrete container type, which must implement {@link DatasourceStartable}
 */
public class DatasourceStartableContainer<T extends GenericContainer<?>>
        extends StartableContainer<T> implements DatasourceStartable {

    private final DatasourceStartable datasourceStartable;
    private volatile DevServicesDatasourceProvider.RunningDevServicesDatasource cachedRunningDatasource;

    public DatasourceStartableContainer(T container) {
        super(container, c -> ((DatasourceStartable) c).getEffectiveJdbcUrl());
        if (!(container instanceof DatasourceStartable)) {
            throw new IllegalArgumentException("Container must implement DatasourceStartable");
        }
        this.datasourceStartable = (DatasourceStartable) container;
    }

    @Override
    public String getPassword() {
        return datasourceStartable.getPassword();
    }

    @Override
    public String getUsername() {
        return datasourceStartable.getUsername();
    }

    @Override
    public String getReactiveUrl() {
        return datasourceStartable.getReactiveUrl();
    }

    @Override
    public String getEffectiveJdbcUrl() {
        return datasourceStartable.getEffectiveJdbcUrl();
    }

    @Override
    public DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevServicesDatasource() {
        if (cachedRunningDatasource == null) {
            cachedRunningDatasource = DatasourceStartable.super.runningDevServicesDatasource();
        }
        return cachedRunningDatasource;
    }
}
