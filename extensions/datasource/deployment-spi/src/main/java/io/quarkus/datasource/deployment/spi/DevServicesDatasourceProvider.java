package io.quarkus.datasource.deployment.spi;

import java.io.Closeable;
import java.util.Map;
import java.util.Optional;

public interface DevServicesDatasourceProvider {

    RunningDevServicesDatasource startDatabase(Optional<String> username, Optional<String> password,
            Optional<String> datasourceName,
            Optional<String> imageName, Map<String, String> additionalProperties);

    class RunningDevServicesDatasource {

        private final String url;
        private final String username;
        private final String password;
        private final Closeable closeTask;

        public RunningDevServicesDatasource(String url, String username, String password, Closeable closeTask) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.closeTask = closeTask;
        }

        public String getUrl() {
            return url;
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
