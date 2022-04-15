package io.quarkus.runtime;

import java.net.BindException;
import java.util.Collections;
import java.util.List;

/**
 * An exception that is meant to stand in for {@link BindException} and provide information
 * about the target which caused the bind exception.
 */
public class QuarkusBindException extends BindException {

    private final List<Integer> ports;

    public QuarkusBindException(int port) {
        this(Collections.singletonList(port));
    }

    public QuarkusBindException(List<Integer> ports) {
        if (ports.isEmpty()) {
            throw new IllegalStateException("ports must not be empty");
        }
        this.ports = ports;
    }

    public List<Integer> getPorts() {
        return ports;
    }
}
