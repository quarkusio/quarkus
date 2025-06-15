package io.quarkus.grpc.server.devmode;

import devmodetest.v1.Devmodetest;
import devmodetest.v1.Devmodetest.DevModeResponse;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class DevModeService implements devmodetest.v1.DevModeService {
    @Override
    public Uni<DevModeResponse> check(Devmodetest.DevModeRequest request) {
        return Uni.createFrom().item(DevModeResponse.getDefaultInstance());
    }

    // test will add override here
    public Multi<DevModeResponse> streamCheck(Devmodetest.DevModeRequest request) {
        return Multi.createFrom()
                .item(DevModeResponse.newBuilder().setStatus(DevModeResponse.Status.NOT_SERVING).build());
    }
}
