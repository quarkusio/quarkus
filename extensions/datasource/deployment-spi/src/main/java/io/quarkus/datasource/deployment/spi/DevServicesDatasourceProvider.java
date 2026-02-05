package io.quarkus.datasource.deployment.spi;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.LaunchMode;

public interface DevServicesDatasourceProvider extends GenericDevServicesDatasourceProvider {

    /**
     * @deprecated implement
     *             {@link #startDatabase(Optional, Optional, String, DevServicesDatasourceContainerConfig, LaunchMode, Optional)}
     *             instead
     */
    @Deprecated(since = "3.3.0", forRemoval = true)
    default RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
            Optional<String> datasourceName,
            DevServicesDatasourceContainerConfig devServicesDatasourceContainerConfig,
            LaunchMode launchMode,
            Optional<Duration> startupTimeout) {
        throw new IllegalStateException(
                "Please implement startDatabase(Optional, Optional, String, DevServicesDatasourceContainerConfig, LaunchMode, Optional)");
    }

    default RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
            String datasourceName,
            DevServicesDatasourceContainerConfig devServicesDatasourceContainerConfig,
            LaunchMode launchMode,
            Optional<Duration> startupTimeout) {
        return startDatabase(username, password,
                DataSourceUtil.isDefault(datasourceName) ? Optional.empty() : Optional.of(datasourceName),
                devServicesDatasourceContainerConfig, launchMode,
                startupTimeout);
    }

    default boolean isDockerRequired() {
        return true;
    }

    record RunningDevServicesDatasource(String id, String jdbcUrl, String reactiveUrl, String username, String password,
            @Deprecated Closeable closeTask) {

        //In the new dev services model, the RunningDevServicesDatasource is used, but its close() method is not
        public RunningDevServicesDatasource(String id, String jdbcUrl, String reactiveUrl, String username, String password) {
            this(id, jdbcUrl, reactiveUrl, username, password, new Closeable() {
                @Override
                public void close() {
                    // No-op
                }
            });
        }
    }

}
