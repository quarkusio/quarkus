package io.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.BindException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class QuarkusBindExceptionTest {

    @ParameterizedTest
    @ValueSource(strings = { "localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]", "::", "[::]" })
    public void loopbackAndWildcardHostsAreKnown(String host) {
        assertTrue(QuarkusBindException.isKnownHost(host));
        assertEquals("Port already bound: 8080: in use",
                new QuarkusBindException(host, 8080, new BindException("in use")).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "192.168.1.10", "fe80::1", "example.com" })
    public void otherHostsAreReported(String host) {
        assertFalse(QuarkusBindException.isKnownHost(host));
        assertEquals("Unable to bind to host: " + host + " and port: 8080: in use",
                new QuarkusBindException(host, 8080, new BindException("in use")).getMessage());
    }
}
