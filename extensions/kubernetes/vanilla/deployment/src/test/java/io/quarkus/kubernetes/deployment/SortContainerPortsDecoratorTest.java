package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;

class SortContainerPortsDecoratorTest {

    @Test
    void testSortPortsWithNullNames() {
        var container = new ContainerBuilder()
                .withName("test-container")
                .withPorts(
                        new ContainerPortBuilder().withName("zebra").withContainerPort(9090).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName(null).withContainerPort(8080).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName("alpha").withContainerPort(9001).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName(null).withContainerPort(8081).withProtocol("TCP").build())
                .build();

        var decorator = new SortContainerPortsDecorator();
        var builder = new ContainerBuilder(container);
        decorator.andThenVisit(builder);

        var sortedPorts = builder.build().getPorts();
        assertEquals(4, sortedPorts.size());

        assertEquals("alpha", sortedPorts.get(0).getName());
        assertEquals(9001, sortedPorts.get(0).getContainerPort());

        assertEquals("zebra", sortedPorts.get(1).getName());
        assertEquals(9090, sortedPorts.get(1).getContainerPort());

        assertNull(sortedPorts.get(2).getName());
        assertEquals(8080, sortedPorts.get(2).getContainerPort());

        assertNull(sortedPorts.get(3).getName());
        assertEquals(8081, sortedPorts.get(3).getContainerPort());
    }

    @Test
    void testSortPortsWithSameNameDifferentPorts() {
        var container = new ContainerBuilder()
                .withName("test-container")
                .withPorts(
                        new ContainerPortBuilder().withName("http").withContainerPort(8443).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName("http").withContainerPort(8080).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName("http").withContainerPort(8081).withProtocol("TCP").build())
                .build();

        var decorator = new SortContainerPortsDecorator();
        var builder = new ContainerBuilder(container);
        decorator.andThenVisit(builder);

        var sortedPorts = builder.build().getPorts();

        assertEquals(8080, sortedPorts.get(0).getContainerPort());
        assertEquals(8081, sortedPorts.get(1).getContainerPort());
        assertEquals(8443, sortedPorts.get(2).getContainerPort());
    }

    @Test
    void testSortPortsWithDifferentProtocols() {
        var container = new ContainerBuilder()
                .withName("test-container")
                .withPorts(
                        new ContainerPortBuilder().withName(null).withContainerPort(8080).withProtocol("UDP").build(),
                        new ContainerPortBuilder().withName(null).withContainerPort(8080).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName(null).withContainerPort(8080).withProtocol("SCTP").build())
                .build();

        var decorator = new SortContainerPortsDecorator();
        var builder = new ContainerBuilder(container);
        decorator.andThenVisit(builder);

        var sortedPorts = builder.build().getPorts();

        assertEquals("SCTP", sortedPorts.get(0).getProtocol());
        assertEquals("TCP", sortedPorts.get(1).getProtocol());
        assertEquals("UDP", sortedPorts.get(2).getProtocol());
    }

    @Test
    void testEmptyPorts() {
        var container = new ContainerBuilder()
                .withName("test-container")
                .withPorts(List.of())
                .build();

        var decorator = new SortContainerPortsDecorator();
        var builder = new ContainerBuilder(container);
        decorator.andThenVisit(builder);

        assertTrue(builder.build().getPorts() == null || builder.build().getPorts().isEmpty());
    }

    @Test
    void testNoPorts() {
        var container = new ContainerBuilder()
                .withName("test-container")
                .build();

        var decorator = new SortContainerPortsDecorator();
        var builder = new ContainerBuilder(container);
        decorator.andThenVisit(builder);

        assertTrue(builder.build().getPorts() == null || builder.build().getPorts().isEmpty());
    }
}
