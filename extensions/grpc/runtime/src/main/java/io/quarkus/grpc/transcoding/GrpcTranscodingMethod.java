package io.quarkus.grpc.transcoding;

/**
 * A metadata class that holds the transcoding information for a gRPC method.
 */
public final class GrpcTranscodingMethod {

    private final String grpcMethodName;
    private final String httpMethodName;
    private final String uriTemplate;

    public GrpcTranscodingMethod(String grpcMethodName, String httpMethodName, String uriTemplate) {
        this.grpcMethodName = grpcMethodName;
        this.httpMethodName = httpMethodName;
        this.uriTemplate = uriTemplate;
    }

    public String getGrpcMethodName() {
        return grpcMethodName;
    }

    public String getHttpMethodName() {
        return httpMethodName;
    }

    public String getUriTemplate() {
        return uriTemplate;
    }
}
