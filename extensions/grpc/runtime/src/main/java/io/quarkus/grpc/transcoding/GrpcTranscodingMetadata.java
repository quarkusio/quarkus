package io.quarkus.grpc.transcoding;

import com.google.protobuf.Message;

import io.grpc.MethodDescriptor;
import io.quarkus.grpc.GrpcTranscodingMarshaller;
import io.vertx.core.http.HttpMethod;

/**
 * A metadata class that holds the transcoding information for a gRPC method.
 *
 * @param <Req> The type of the request message.
 * @param <Resp> The type of the response message.
 */
public class GrpcTranscodingMetadata<Req extends Message, Resp extends Message> {

    private final HttpMethod httpMethod;
    private final String uriTemplate;
    private final String grpcMethodName;
    private final GrpcTranscodingMarshaller<Req> requestMarshaller;
    private final GrpcTranscodingMarshaller<Resp> responseMarshaller;
    private final MethodDescriptor<Req, Resp> methodDescriptor;

    public GrpcTranscodingMetadata(HttpMethod httpMethod,
            String uriTemplate,
            String grpcMethodName,
            GrpcTranscodingMarshaller<Req> requestMarshaller,
            GrpcTranscodingMarshaller<Resp> responseMarshaller, MethodDescriptor<Req, Resp> methodDescriptor) {
        this.httpMethod = httpMethod;
        this.uriTemplate = uriTemplate;
        this.grpcMethodName = grpcMethodName;
        this.requestMarshaller = requestMarshaller;
        this.responseMarshaller = responseMarshaller;
        this.methodDescriptor = methodDescriptor;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getUriTemplate() {
        return uriTemplate;
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

    public String getUriAsRegex() {
        return uriTemplate.replaceAll("\\{[^/]+\\}", "[^/]+");
    }
}
