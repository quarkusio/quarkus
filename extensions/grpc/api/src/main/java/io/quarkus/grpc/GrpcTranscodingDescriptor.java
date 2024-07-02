package io.quarkus.grpc;

public class GrpcTranscodingDescriptor<Req extends com.google.protobuf.Message, Resp extends com.google.protobuf.Message> {

    private final GrpcTranscodingMarshaller<Req> requestMarshaller;
    private final GrpcTranscodingMarshaller<Resp> responseMarshaller;

    public GrpcTranscodingDescriptor(GrpcTranscodingMarshaller<Req> requestMarshaller,
            GrpcTranscodingMarshaller<Resp> responseMarshaller) {
        this.requestMarshaller = requestMarshaller;
        this.responseMarshaller = responseMarshaller;
    }

    public GrpcTranscodingMarshaller<Req> getRequestMarshaller() {
        return requestMarshaller;
    }

    public GrpcTranscodingMarshaller<Resp> getResponseMarshaller() {
        return responseMarshaller;
    }
}
