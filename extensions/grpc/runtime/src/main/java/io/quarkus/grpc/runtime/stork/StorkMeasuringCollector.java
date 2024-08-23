package io.quarkus.grpc.runtime.stork;

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Context;
import io.smallrye.stork.api.ServiceInstance;

interface StorkMeasuringCollector {
    Context.Key<AtomicReference<ServiceInstance>> STORK_SERVICE_INSTANCE = Context.key("stork.service-instance");
    Context.Key<Boolean> STORK_MEASURE_TIME = Context.key("stork.measure-time");

    void recordReply();

    void recordEnd(Throwable error);
}
