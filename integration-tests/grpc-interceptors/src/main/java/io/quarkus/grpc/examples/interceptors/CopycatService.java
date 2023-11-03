package io.quarkus.grpc.examples.interceptors;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyCopycatGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class CopycatService extends MutinyCopycatGrpc.CopycatImplBase {

    private HelloReply getReply(HelloRequest request) {
        String name = request.getName();
        if (name.equals("Fail")) {
            throw new HelloException(name);
        }
        return HelloReply.newBuilder().setMessage("Hello " + name).build();
    }

    @Override
    public Uni<HelloReply> sayCat(HelloRequest request) {
        return Uni.createFrom().item(getReply(request));
    }

    @Override
    public Multi<HelloReply> multiCat(Multi<HelloRequest> request) {
        return request.map(this::getReply);
    }
}
