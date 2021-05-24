package io.quarkus.grpc.runtime.health;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import grpc.health.v1.HealthOuterClass;
import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import grpc.health.v1.MutinyHealthGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.supports.context.GrpcEnableRequestContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

// Note that we need to add the scope and interceptor binding explicitly because this class is not part of the index
@Singleton
@GrpcEnableRequestContext
@GrpcService
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
                Multi.createFrom().item(new Supplier<HealthOuterClass.HealthCheckResponse>() {
                    @Override
                    public HealthOuterClass.HealthCheckResponse get() {
                        return healthStorage.statusForService(service);
                    }
                }),
                broadcastProcessor.map(new Function<ServingStatus, HealthOuterClass.HealthCheckResponse>() {
                    @Override
                    public HealthOuterClass.HealthCheckResponse apply(ServingStatus servingStatus) {
                        return healthStorage.resultForStatus(servingStatus);
                    }
                })).skip().repetitions();
    }
}
