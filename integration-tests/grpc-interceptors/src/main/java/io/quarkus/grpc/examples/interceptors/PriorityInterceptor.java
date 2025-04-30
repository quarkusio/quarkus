package io.quarkus.grpc.examples.interceptors;

import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.ServerInterceptor;

public interface PriorityInterceptor extends ServerInterceptor, Prioritized {
}
