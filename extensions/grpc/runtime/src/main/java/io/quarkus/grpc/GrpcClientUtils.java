package io.quarkus.grpc;

import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.quarkus.grpc.runtime.MutinyClient;

/**
 * gRPC client utilities
 */
public class GrpcClientUtils {

    /**
     * Attach headers to a gRPC client.
     *
     * To make a call with headers, first invoke this method and then perform the intended call with the <b>returned</b> client
     *
     * @param client any kind of gRPC client
     * @param extraHeaders headers to attach
     * @param <T> type of the client
     * @return a client with headers attached
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T attachHeaders(T client, Metadata extraHeaders) {
        if (client == null) {
            throw new NullPointerException("Cannot attach headers to a null client");
        }
        if (client instanceof AbstractStub) {
            return (T) MetadataUtils.attachHeaders((AbstractStub) client, extraHeaders);
        } else if (client instanceof MutinyClient) {
            MutinyClient mutinyClient = (MutinyClient) client;
            AbstractStub stub = MetadataUtils.attachHeaders(mutinyClient.getStub(), extraHeaders);
            return (T) ((MutinyClient) client).newInstanceWithStub(stub);
        } else {
            throw new IllegalArgumentException("Unsupported client type " + client.getClass());
        }
    }
}
