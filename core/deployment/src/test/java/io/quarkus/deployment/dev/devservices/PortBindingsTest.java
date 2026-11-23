package io.quarkus.deployment.dev.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.dev.devservices.ContainerInfo.ContainerPort;

class PortBindingsTest {

    @Test
    void isIpv6() {
        assertTrue(PortBindings.isIpv6("::"));
        assertTrue(PortBindings.isIpv6("fe80::1"));
        assertFalse(PortBindings.isIpv6("0.0.0.0"));
        assertFalse(PortBindings.isIpv6(null));
    }

    @Test
    void selectBindingPrefersIpv4() {
        ContainerPort[] ports = {
                new ContainerPort("::", 9200, 55002, "tcp"),
                new ContainerPort("0.0.0.0", 9300, 55003, "tcp"),
                new ContainerPort("0.0.0.0", 9200, 55001, "tcp"),
        };
        assertEquals(Optional.of(55001), PortBindings.selectBinding(ports, 9200).map(ContainerPort::publicPort));
        assertEquals(Optional.of(55003), PortBindings.selectBinding(ports, 9300).map(ContainerPort::publicPort));
        assertEquals(Optional.empty(), PortBindings.selectBinding(ports, 1234));
        assertEquals(Optional.empty(), PortBindings.selectBinding(null, 9200));
    }

    @Test
    void selectBindingFallsBackToIpv6AndIgnoresUnpublished() {
        ContainerPort[] ports = {
                new ContainerPort(null, 9200, null, "tcp"),
                new ContainerPort("::", 9200, 55002, "tcp"),
        };
        assertEquals(Optional.of(55002), PortBindings.selectBinding(ports, 9200).map(ContainerPort::publicPort));
    }

    @Test
    void selectBindingsKeepsOnePerPrivatePort() {
        ContainerPort[] ports = {
                new ContainerPort("::", 9200, 55002, "tcp"),
                new ContainerPort("0.0.0.0", 9200, 55001, "tcp"),
                new ContainerPort("::", 9300, 55004, "tcp"),
                new ContainerPort("0.0.0.0", 9400, null, "tcp"),
        };
        List<Integer> publicPorts = PortBindings.selectBindings(ports).stream().map(ContainerPort::publicPort).toList();
        assertEquals(List.of(55001, 55004), publicPorts);
    }
}
