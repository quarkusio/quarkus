package io.quarkus.test.common;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * A launcher that simply sets the {@code quarkus.http.host} property based on the value {@code quarkus.http.test-host}
 * in order to support the case of running integration tests against an already running application
 * using RestAssured without any chances.
 */
@SuppressWarnings("rawtypes")
public class TestHostLauncher implements ArtifactLauncher {
    private String previousHost;

    @Override
    public Optional<ListeningAddress> start() throws IOException {
        Config config = ConfigProvider.getConfig();
        // set 'quarkus.http.host' to ensure that RestAssured targets the proper host
        previousHost = System.setProperty("quarkus.http.host", config.getValue("quarkus.http.test-host", String.class));

        // We need to manually query and set defaults, because this runs in IT and VertxHttpConfig is not available
        boolean testSslEnabled = config.getOptionalValue("quarkus.http.test-ssl-enabled", boolean.class).orElse(false);
        int port;
        String protocol;
        if (testSslEnabled) {
            port = config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(8444);
            protocol = "https";
        } else {
            port = config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(8081);
            protocol = "http";
        }
        return Optional.of(new ListeningAddress(port, protocol));
    }

    @Override
    public void close() throws IOException {
        if (previousHost != null) {
            System.setProperty("quarkus.http.host", previousHost);
        }
    }

    @Override
    public void includeAsSysProps(Map systemProps) {

    }

    @Override
    public void init(InitContext initContext) {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        throw new IllegalStateException("Should not be called");
    }

}
