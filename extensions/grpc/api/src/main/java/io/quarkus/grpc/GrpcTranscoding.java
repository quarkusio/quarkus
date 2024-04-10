package io.quarkus.grpc;

import com.google.protobuf.Message;

public interface GrpcTranscoding {

    String getGrpcServiceName();

    <Req extends Message, Resp extends Message> GrpcTranscodingDescriptor<Req, Resp> findTranscodingDescriptor(
            String methodName);
}
