package io.quarkus.datasource.deployment.spi;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.LaunchMode;

public interface DevServicesDatasourceProvider {

    RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
            Optional<String> datasourceName,
            DevServicesDatasourceContainerConfig devServicesDatasourceContainerConfig,
            LaunchMode launchMode,
            Optional<Duration> startupTimeout);

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
