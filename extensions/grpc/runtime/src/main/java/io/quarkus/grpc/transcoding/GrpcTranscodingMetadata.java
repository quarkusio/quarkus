package io.quarkus.grpc.transcoding;

import com.google.protobuf.Message;

import io.grpc.MethodDescriptor;
import io.quarkus.grpc.GrpcTranscodingMarshaller;

public class GrpcTranscodingMetadata<Req extends Message, Resp extends Message> {

    private final String httpMethodName;
    private final String grpcMethodName;
    private final GrpcTranscodingMarshaller<Req> requestMarshaller;
    private final GrpcTranscodingMarshaller<Resp> responseMarshaller;
    private final MethodDescriptor<Req, Resp> methodDescriptor;

    public GrpcTranscodingMetadata(String httpMethodName, String grpcMethodName,
            GrpcTranscodingMarshaller<Req> requestMarshaller,
            GrpcTranscodingMarshaller<Resp> responseMarshaller, MethodDescriptor<Req, Resp> methodDescriptor) {
        this.httpMethodName = httpMethodName;
        this.grpcMethodName = grpcMethodName;
        this.requestMarshaller = requestMarshaller;
        this.responseMarshaller = responseMarshaller;
        this.methodDescriptor = methodDescriptor;
    }

    public String getHttpMethodName() {
        return httpMethodName;
    }

    public String getGrpcMethodName() {
        return grpcMethodName;
    }

    public GrpcTranscodingMarshaller<Req> getRequestMarshaller() {
        return requestMarshaller;
    }

    public GrpcTranscodingMarshaller<Resp> getResponseMarshaller() {
        return responseMarshaller;
    }

    public MethodDescriptor<Req, Resp> getMethodDescriptor() {
        return methodDescriptor;
    }
}
