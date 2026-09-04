package io.quarkus.grpc.transcoding;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TranscodingServiceImpl extends MutinyTranscodingServiceGrpc.TranscodingServiceImplBase {

    @Override
    public Uni<EchoResponse> getSinglePath(SinglePathRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setItemId(request.getItemId())
                        .build());
    }

    @Override
    public Uni<EchoResponse> getMultiPath(MultiPathRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setUserId(request.getUserId())
                        .setItemId(request.getItemId())
                        .build());
    }

    @Override
    public Uni<EchoResponse> getPathQuery(PathQueryRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setItemId(request.getItemId())
                        .setRevision(request.getRevision())
                        .build());
    }

    @Override
    public Uni<EchoResponse> getMultiPathQuery(MultiPathQueryRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setUserId(request.getUserId())
                        .setItemId(request.getItemId())
                        .setColor(request.getColor())
                        .setSize(request.getSize())
                        .build());
    }

    @Override
    public Uni<EchoResponse> postPathBody(PathBodyRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setItemId(request.getItemId())
                        .setName(request.getItem().getName())
                        .setDescription(request.getItem().getDescription())
                        .build());
    }

    @Override
    public Uni<EchoResponse> postPathQueryBody(PathQueryBodyRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setUserId(request.getUserId())
                        .setItemId(request.getItemId())
                        .setFilter(request.getFilter())
                        .setName(request.getItem().getName())
                        .setDescription(request.getItem().getDescription())
                        .build());
    }

    @Override
    public Uni<EchoResponse> putPathStarBody(PathStarBodyRequest request) {
        return Uni.createFrom().item(
                EchoResponse.newBuilder()
                        .setItemId(request.getItemId())
                        .setName(request.getName())
                        .setDescription(request.getDescription())
                        .build());
    }
}
