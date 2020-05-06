package io.quarkus.grpc.runtime.health;

import javax.inject.Inject;
import javax.inject.Singleton;

import grpc.health.v1.HealthOuterClass;
import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import grpc.health.v1.MutinyHealthGrpc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@Singleton
public class GrpcHealthEndpoint extends MutinyHealthGrpc.HealthImplBase {
    @Inject
    GrpcHealthStorage healthStorage;

    @Override
    public Uni<HealthOuterClass.HealthCheckResponse> check(HealthOuterClass.HealthCheckRequest request) {
        return Uni.createFrom().item(healthStorage.statusForService(request.getService()));
    }

    @Override
    public Multi<HealthOuterClass.HealthCheckResponse> watch(HealthOuterClass.HealthCheckRequest request) {
        String service = request.getService();

        BroadcastProcessor<ServingStatus> broadcastProcessor = healthStorage.createStatusBroadcastProcessor(service);
        return Multi.createBy().concatenating().streams(
                Multi.createFrom().item(() -> healthStorage.statusForService(service)),
                // TODO: if there's no service, we still create a broadcast processor here.
                // TODO: does it make sense resource-wise?
                broadcastProcessor.map(healthStorage::resultForStatus)).transform().byDroppingRepetitions();
    }
}
