package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Objects;

public class EndpointConfig {

    private final String path;
    private final String verb;
    private final String consumes;
    private final String produces;
    private final String exposingMethod;

    public EndpointConfig(String path, String verb, String consumes, String produces, String exposingMethod) {
        this.path = path;
        this.verb = verb;
        this.consumes = consumes != null ? consumes : "*";
        this.produces = produces != null ? produces : "*";
        this.exposingMethod = exposingMethod;
    }

    @Override
    public String toString() {
        return String.format("consumes %s, produces %s", consumes, produces);
    }

    public String toCompleteString() {
        return String.format("%s consumes %s, produces %s", exposingMethod, consumes, produces);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EndpointConfig that = (EndpointConfig) o;
        return Objects.equals(path, that.path) && Objects.equals(verb, that.verb) && Objects.equals(consumes, that.consumes)
                && Objects.equals(produces, that.produces) && Objects.equals(exposingMethod, that.exposingMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, verb, consumes, produces, exposingMethod);
    }

    public String getExposedEndpoint() {
        return String.format("%s %s", verb, path);
    }
}
