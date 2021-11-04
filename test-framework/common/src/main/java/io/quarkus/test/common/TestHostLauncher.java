package io.quarkus.test.common;

import java.io.IOException;
import java.util.Map;

/**
 * A launcher that simply sets the {@code quarkus.http.host} property based on the value {@code quarkus.http.test-host}
 * in order to support the case of running integration tests against an already running application
 * using RestAssured without any chances.
 *
 * This is highly experimental, so changes are to be expected.
 */
@SuppressWarnings("rawtypes")
public class TestHostLauncher implements ArtifactLauncher {

    private String previousHost;

    @Override
    public void start() throws IOException {
        // set 'quarkus.http.host' to ensure that RestAssured targets the proper host
        previousHost = System.setProperty("quarkus.http.host", System.getProperty("quarkus.http.test-host"));
    }

    @Override
    public void close() throws IOException {
        if (previousHost != null) {
            System.setProperty("quarkus.http.host", previousHost);
        }
    }

    @Override
    public boolean listensOnSsl() {
        return false;
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
