package io.quarkus.grpc;

import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.quarkus.arc.ClientProxy;

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

        client = getProxiedObject(client);

        if (client instanceof AbstractStub) {
            return (T) ((AbstractStub) client).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders));
        } else if (client instanceof MutinyClient) {
            MutinyClient mutinyClient = (MutinyClient) client;
            AbstractStub stub = mutinyClient.getStub()
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders));
            return (T) ((MutinyClient) client).newInstanceWithStub(stub);
        } else {
            throw new IllegalArgumentException("Unsupported client type " + client.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxiedObject(T client) {
        // If we get a proxy, get the actual instance.
        if (client instanceof ClientProxy) {
            client = (T) ((ClientProxy) client).arc_contextualInstance();
        }
        return client;
    }
}
