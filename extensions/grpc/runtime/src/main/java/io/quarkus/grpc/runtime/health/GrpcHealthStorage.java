package io.quarkus.grpc.runtime.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import grpc.health.v1.HealthOuterClass.HealthCheckResponse;
import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class GrpcHealthStorage {

    public static final String DEFAULT_SERVICE_NAME = "";

    private final Map<String, ServingStatus> statuses = new ConcurrentHashMap<>();
    private final Map<String, BroadcastProcessor<ServingStatus>> statusBroadcasters = new ConcurrentHashMap<>();

    public GrpcHealthStorage() {
        BroadcastProcessor<ServingStatus> broadcastProcessor = BroadcastProcessor.create();
        broadcastProcessor.subscribe().with(new Consumer<ServingStatus>() {
            @Override
            public void accept(ServingStatus status) {
                statuses.put(DEFAULT_SERVICE_NAME, status);
            }
        });

        statusBroadcasters.put(DEFAULT_SERVICE_NAME, broadcastProcessor);
        broadcastProcessor.onNext(ServingStatus.NOT_SERVING);
    }

    public void setStatus(String service, ServingStatus status) {
        String serviceName = service == null ? DEFAULT_SERVICE_NAME : service;

        BroadcastProcessor<ServingStatus> broadcastProcessor = statusBroadcasters.computeIfAbsent(serviceName,
                new Function<String, BroadcastProcessor<ServingStatus>>() {
                    @Override
                    public BroadcastProcessor<ServingStatus> apply(String sn) {
                        return GrpcHealthStorage.this.createBroadcastProcessor(sn);
                    }
                });
        broadcastProcessor.onNext(status);
    }

    public Map<String, ServingStatus> getStatuses() {
        return statuses;
    }

    void shutdown(@Observes ShutdownEvent e) {
        for (BroadcastProcessor<ServingStatus> servingStatusBroadcastProcessor : statusBroadcasters.values()) {
            servingStatusBroadcastProcessor.onComplete();
        }
    }

    private BroadcastProcessor<ServingStatus> createBroadcastProcessor(String serviceName) {
        BroadcastProcessor<ServingStatus> processor = BroadcastProcessor.create();
        processor.subscribe().with(new Consumer<ServingStatus>() {
            @Override
            public void accept(ServingStatus status) {
                statuses.put(serviceName, status);
            }
        });
        return processor;
    }

    public HealthCheckResponse statusForService(String serviceName) {
        ServingStatus servingStatus = statuses.getOrDefault(serviceName, ServingStatus.UNKNOWN);
        return resultForStatus(servingStatus);
    }

    public HealthCheckResponse resultForStatus(ServingStatus servingStatus) {
        return HealthCheckResponse.newBuilder()
                .setStatus(servingStatus)
                .build();
    }

    BroadcastProcessor<ServingStatus> createStatusBroadcastProcessor(String serviceName) {
        return statusBroadcasters.computeIfAbsent(serviceName,
                new Function<String, BroadcastProcessor<ServingStatus>>() {
                    @Override
                    public BroadcastProcessor<ServingStatus> apply(String sn) {
                        return GrpcHealthStorage.this.createBroadcastProcessor(sn);
                    }
                });
    }
}
