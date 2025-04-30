package io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc;

import static io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR;

import java.net.SocketAddress;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class GrpcRequest {

    public static GrpcRequest server(
            final MethodDescriptor<?, ?> methodDescriptor,
            final Metadata metadata,
            final Attributes attributes,
            final String authority) {

        return new GrpcRequest(methodDescriptor,
                metadata,
                attributes,
                attributes == null ? null : attributes.get(TRANSPORT_ATTR_REMOTE_ADDR),
                authority);
    }

    public static GrpcRequest client(final MethodDescriptor<?, ?> methodDescriptor, String authority) {
        return new GrpcRequest(methodDescriptor, null, null, null, authority);
    }

    public static GrpcRequest client(final MethodDescriptor<?, ?> methodDescriptor, final Metadata metadata) {
        return new GrpcRequest(methodDescriptor, metadata, null, null, null);
    }

    private final MethodDescriptor<?, ?> methodDescriptor;
    private Metadata metadata;
    private final Attributes attributes;

    private volatile String logicalHost;
    private volatile int logicalPort = -1;
    private volatile SocketAddress peerSocketAddress;

    private GrpcRequest(
            final MethodDescriptor<?, ?> methodDescriptor,
            final Metadata metadata,
            final Attributes attributes,
            final SocketAddress peerSocketAddress,
            final String authority) {
        this.methodDescriptor = methodDescriptor;
        this.metadata = metadata;
        this.attributes = attributes;
        this.peerSocketAddress = peerSocketAddress;
        setLogicalAddress(authority);
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getLogicalHost() {
        return logicalHost;
    }

    public int getLogicalPort() {
        return logicalPort;
    }

    public SocketAddress getPeerSocketAddress() {
        return peerSocketAddress;
    }

    void setPeerSocketAddress(SocketAddress peerSocketAddress) {
        this.peerSocketAddress = peerSocketAddress;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    private void setLogicalAddress(String authority) {
        if (authority == null) {
            return;
        }
        int index = authority.indexOf(':');
        if (index == -1) {
            logicalHost = authority;
        } else {
            logicalHost = authority.substring(0, index);
            try {
                logicalPort = Integer.parseInt(authority.substring(index + 1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }
}
