package io.quarkus.grpc.external.proto;

import com.test.MyProto.TextContainer;
import com.test.MyTest;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class MyGrpcService implements MyTest {

    @Override
    public Uni<TextContainer> doTest(TextContainer request) {
        String response = "reply_to:" + request.getText();
        return Uni.createFrom().item(TextContainer.newBuilder().setText(response).build());
    }
}
