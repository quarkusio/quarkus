package io.quarkus.opentelemetry.runtime.config;

import static io.quarkus.opentelemetry.runtime.config.HierarchicalOTelConnectionConfigInterceptor.BASE;
import static io.quarkus.opentelemetry.runtime.config.HierarchicalOTelConnectionConfigInterceptor.METRICS;
import static io.quarkus.opentelemetry.runtime.config.HierarchicalOTelConnectionConfigInterceptor.MappingFunction;
import static io.quarkus.opentelemetry.runtime.config.HierarchicalOTelConnectionConfigInterceptor.TRACES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HierarchicalOTelConnectionConfigInterceptorTest {

    private final static Map<String, String> FALLBACKS = new HashMap<>();

    static {
        FALLBACKS.put(TRACES + "endpoint", BASE + "endpoint");
        FALLBACKS.put(METRICS + "endpoint", BASE + "endpoint");
        FALLBACKS.put(TRACES + "headers", BASE + "headers");
        FALLBACKS.put(METRICS + "headers", BASE + "headers");
        FALLBACKS.put(TRACES + "compression", BASE + "compression");
        FALLBACKS.put(METRICS + "compression", BASE + "compression");
        FALLBACKS.put(TRACES + "timeout", BASE + "timeout");
        FALLBACKS.put(METRICS + "timeout", BASE + "timeout");
        FALLBACKS.put(TRACES + "protocol", BASE + "protocol");
        FALLBACKS.put(METRICS + "protocol", BASE + "protocol");
        FALLBACKS.put(TRACES + "key-cert.keys", BASE + "key-cert.keys");
        FALLBACKS.put(METRICS + "key-cert.keys", BASE + "key-cert.keys");
        FALLBACKS.put(TRACES + "key-cert.certs", BASE + "key-cert.certs");
        FALLBACKS.put(METRICS + "key-cert.certs", BASE + "key-cert.certs");
        FALLBACKS.put(TRACES + "trust-cert.certs", BASE + "trust-cert.certs");
        FALLBACKS.put(METRICS + "trust-cert.certs", BASE + "trust-cert.certs");
        FALLBACKS.put(TRACES + "tls-configuration-name", BASE + "tls-configuration-name");
        FALLBACKS.put(METRICS + "tls-configuration-name", BASE + "tls-configuration-name");
        FALLBACKS.put(TRACES + "proxy-options.enabled", BASE + "proxy-options.enabled");
        FALLBACKS.put(METRICS + "proxy-options.enabled", BASE + "proxy-options.enabled");
        FALLBACKS.put(TRACES + "proxy-options.username", BASE + "proxy-options.username");
        FALLBACKS.put(METRICS + "proxy-options.username", BASE + "proxy-options.username");
        FALLBACKS.put(TRACES + "proxy-options.password", BASE + "proxy-options.password");
        FALLBACKS.put(METRICS + "proxy-options.password", BASE + "proxy-options.password");
        FALLBACKS.put(TRACES + "proxy-options.port", BASE + "proxy-options.port");
        FALLBACKS.put(METRICS + "proxy-options.port", BASE + "proxy-options.port");
        FALLBACKS.put(TRACES + "proxy-options.host", BASE + "proxy-options.host");
        FALLBACKS.put(METRICS + "proxy-options.host", BASE + "proxy-options.host");
    }

    @Test
    void testMapping() {
        MappingFunction mappingFunction = new MappingFunction();
        for (String propertyName : FALLBACKS.keySet()) {
            assertEquals(FALLBACKS.get(propertyName), mappingFunction.apply(propertyName));
        }
    }

    @Test
    void testNotMapping() {
        MappingFunction mappingFunction = new MappingFunction();
        assertEquals("something", mappingFunction.apply("something"));
    }
}
