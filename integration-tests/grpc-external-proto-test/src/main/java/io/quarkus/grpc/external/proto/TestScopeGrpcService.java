package io.quarkus.grpc.external.proto;

import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.external.testscope.TestScopeMessage;
import io.quarkus.grpc.external.testscope.TestScopeService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TestScopeGrpcService implements TestScopeService {

    @Override
    public Uni<TestScopeMessage> send(TestScopeMessage request) {
        return Uni.createFrom().item(
                TestScopeMessage.newBuilder().setContent("reply:" + request.getContent()).build());
    }
}
