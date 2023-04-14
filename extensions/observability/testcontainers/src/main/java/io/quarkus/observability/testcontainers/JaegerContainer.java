package io.quarkus.observability.testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractContainerConfig;
import io.quarkus.observability.common.config.JaegerConfig;

@SuppressWarnings("resource")
public class JaegerContainer extends ObservabilityContainer<JaegerContainer, JaegerConfig> {
    public static final int JAEGER_ENDPOINT_PORT = 14250;
    public static final int JAEGER_CONSOLE_PORT = 16686;

    public JaegerContainer() {
        this(new JaegerConfigImpl());
    }

    public JaegerContainer(JaegerConfig config) {
        super(config);
        withExposedPorts(JAEGER_ENDPOINT_PORT, JAEGER_CONSOLE_PORT);

        LogMessageWaitStrategy lmws = new LogMessageWaitStrategy();
        waitingFor(lmws.withRegEx(
                ".*\"Health Check state change\",\"status\":\"ready\".*")
                .withStartupTimeout(Duration.of(15L, ChronoUnit.SECONDS)));
    }

    public int getJaegerEndpointPort() {
        return getMappedPort(JAEGER_ENDPOINT_PORT);
    }

    public int getJaegerConsolePort() {
        return getMappedPort(JAEGER_CONSOLE_PORT);
    }

    private static class JaegerConfigImpl extends AbstractContainerConfig implements JaegerConfig {
        public JaegerConfigImpl() {
            super(ContainerConstants.JAEGER);
        }

        @Override
        public Optional<Set<String>> networkAliases() {
            return Optional.of(Set.of("jaeger"));
        }
    }

}
