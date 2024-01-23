package io.quarkus.grpc.example.multiplex;

import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.MutinyMultiplexGrpc;
import io.grpc.examples.multiplex.StringRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;

@GrpcService
public class MultiplexService extends MutinyMultiplexGrpc.MultiplexImplBase {
    public Multi<LongReply> parse(Multi<StringRequest> request) {
        return request
                .map(x -> LongReply.newBuilder().setValue(Long.parseLong(x.getNumber())).build());
    }
}
