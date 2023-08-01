package io.quarkus.datasource.deployment.spi;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.LaunchMode;

public interface DevServicesDatasourceProvider {

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

    class RunningDevServicesDatasource {

        private final String id;
        private final String jdbcUrl;
        private final String reactiveUrl;
        private final String username;
        private final String password;
        private final Closeable closeTask;

        public RunningDevServicesDatasource(String id, String jdbcUrl, String reactiveUrl, String username, String password,
                Closeable closeTask) {
            this.id = id;
            this.jdbcUrl = jdbcUrl;
            this.reactiveUrl = reactiveUrl;
            this.username = username;
            this.password = password;
            this.closeTask = closeTask;
        }

        public String getId() {
            return id;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public String getReactiveUrl() {
            return reactiveUrl;
        }

        public Closeable getCloseTask() {
            return closeTask;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

    }

}
