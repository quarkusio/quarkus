package io.quarkus.runtime;

import java.net.BindException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An exception that is meant to stand in for {@link BindException} and provide information
 * about the target which caused the bind exception.
 */
public class QuarkusBindException extends BindException {

    private final List<Integer> ports;

    private static String createMessage(List<Integer> ports) {
        return "Port(s) already bound: " + ports.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(", "));
    }

    private static void assertPortsNotEmpty(List<Integer> ports) {
        if (ports.isEmpty()) {
            throw new IllegalStateException("ports must not be empty");
        }
    }

    public QuarkusBindException(Integer... ports) {
        this(Arrays.asList(ports));
    }

    public QuarkusBindException(List<Integer> ports) {
        super(createMessage(ports));
        assertPortsNotEmpty(ports);
        this.ports = ports;
    }

    public QuarkusBindException(BindException e, Integer... ports) {
        this(e, Arrays.asList(ports));
    }

    public QuarkusBindException(BindException e, List<Integer> ports) {
        super(createMessage(ports) + ": " + e.getMessage());
        assertPortsNotEmpty(ports);
        this.ports = ports;
    }

    public List<Integer> getPorts() {
        return ports;
    }
}
