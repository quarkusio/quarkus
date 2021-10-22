package io.quarkus.kafka.streams.runtime;

import java.util.function.Supplier;

import org.apache.kafka.streams.Topology;

import io.quarkus.arc.Arc;

public class TopologySupplier implements Supplier<Topology> {

    @Override
    public Topology get() {
        return Arc.container().instance(Topology.class).get();
    }
}
