package io.quarkus.grpc.transcoding;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.Status;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.impl.Utils;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpc.server.GrpcServiceBridge;

public class GrpcTranscodingBridge implements GrpcServiceBridge {

    private final List<ServerMethodDefinition<?, ?>> methods;

    public GrpcTranscodingBridge(List<ServerMethodDefinition<?, ?>> methods) {
        this.methods = methods;
    }

    @Override
    public void unbind(GrpcServer server) {
        methods.forEach(m -> unbind((GrpcTranscodingServer) server, m));
    }

    private <Req, Resp> void unbind(GrpcTranscodingServer server, ServerMethodDefinition<Req, Resp> methodDef) {
        server.callHandler(methodDef.getMethodDescriptor(), null);
    }

    @Override
    public void bind(GrpcServer server) {
        methods.forEach(m -> bind((GrpcTranscodingServer) server, m));
    }

    private <Req, Resp> void bind(GrpcTranscodingServer server, ServerMethodDefinition<Req, Resp> methodDef) {
        server.callHandler(methodDef.getMethodDescriptor(), req -> {
            ServerCallHandler<Req, Resp> callHandler = methodDef.getServerCallHandler();
            MethodDescriptor.Marshaller<Req> reqMarshaller = server
                    .findRequestMarshaller(methodDef.getMethodDescriptor().getFullMethodName());
            MethodDescriptor.Marshaller<Resp> respMarshaller = server
                    .findResponseMarshaller(methodDef.getMethodDescriptor().getFullMethodName());

            GrpcTranscodingBridge.ServerCallImpl<Req, Resp> call = new GrpcTranscodingBridge.ServerCallImpl<>(req, methodDef,
                    reqMarshaller, respMarshaller);

            ServerCall.Listener<Req> listener = callHandler.startCall(call, Utils.readMetadata(req.headers()));
            call.init(listener);
        });
    }

    private static class ServerCallImpl<Req, Resp> extends ServerCall<Req, Resp> {

        private final GrpcTranscodingRequest<Req, Resp> req;
        private final ServerMethodDefinition<Req, Resp> methodDef;

        private final MethodDescriptor.Marshaller<Req> reqMarshaller;
        private final MethodDescriptor.Marshaller<Resp> respMarshaller;

        private final GrpcTranscodingReadStreamAdapter<Req> readAdapter;
        private final GrpcTranscodingWriteStreamAdapter<Resp> writeAdapter;
        private ServerCall.Listener<Req> listener;
        private boolean halfClosed;
        private boolean closed;
        private int messagesSent;
        private final Attributes attributes;

        public ServerCallImpl(GrpcServerRequest<Req, Resp> req, ServerMethodDefinition<Req, Resp> methodDef,
                MethodDescriptor.Marshaller<Req> reqMarshaller, MethodDescriptor.Marshaller<Resp> respMarshaller) {
            this.req = (GrpcTranscodingRequest<Req, Resp>) req;
            this.methodDef = methodDef;
            this.reqMarshaller = reqMarshaller;
            this.respMarshaller = respMarshaller;
            this.readAdapter = new GrpcTranscodingReadStreamAdapter<Req>() {
                @Override
                protected void handleClose() {
                    halfClosed = true;
                    listener.onHalfClose();
                }

                @Override
                protected void handleMessage(Req msg) {
                    listener.onMessage(msg);
                }
            };
            this.writeAdapter = new GrpcTranscodingWriteStreamAdapter<Resp>() {
                @Override
                protected void handleReady() {
                    listener.onReady();
                }
            };
            this.attributes = createAttributes();
        }

        void init(ServerCall.Listener<Req> listener) {
            this.listener = listener;
            req.errorHandler(error -> {
                if (error == GrpcError.CANCELLED && !closed) {
                    listener.onCancel();
                }
            });

            readAdapter.init(req, new GrpcTranscodingMessageDecoder<>(reqMarshaller));
            writeAdapter.init(req.response(), new GrpcTranscodingMessageEncoder<>(respMarshaller));
        }

        private Attributes createAttributes() {
            Attributes.Builder builder = Attributes.newBuilder();
            SocketAddress remoteAddr = req.connection().remoteAddress();
            if (remoteAddr != null && remoteAddr.isInetSocket()) {
                try {
                    InetAddress address = InetAddress.getByName(remoteAddr.hostAddress());
                    builder.set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, new InetSocketAddress(address, remoteAddr.port()));
                } catch (UnknownHostException ignored) {
                }
            }
            SocketAddress localAddr = req.connection().localAddress();
            if (localAddr != null && localAddr.isInetSocket()) {
                try {
                    InetAddress address = InetAddress.getByName(localAddr.hostAddress());
                    builder.set(Grpc.TRANSPORT_ATTR_LOCAL_ADDR, new InetSocketAddress(address, localAddr.port()));
                } catch (UnknownHostException ignored) {
                }
            }
            if (req.connection().isSsl()) {
                builder.set(Grpc.TRANSPORT_ATTR_SSL_SESSION, req.connection().sslSession());
            }
            return builder.build();
        }

        @Override
        public boolean isReady() {
            return writeAdapter.isReady();
        }

        @Override
        public void request(int numMessages) {
            readAdapter.request(numMessages);
        }

        @Override
        public void sendHeaders(Metadata headers) {
            Utils.writeMetadata(headers, req.response().headers());
        }

        @Override
        public void sendMessage(Resp message) {
            messagesSent++;
            writeAdapter.write(message);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            if (closed) {
                throw new IllegalStateException("Already closed");
            }
            closed = true;
            GrpcServerResponse<Req, Resp> response = req.response();
            if (status == Status.OK && methodDef.getMethodDescriptor().getType().serverSendsOneMessage() && messagesSent == 0) {
                response.status(GrpcStatus.UNAVAILABLE).end();
            } else {
                Utils.writeMetadata(trailers, response.trailers());
                response.status(GrpcStatus.valueOf(status.getCode().value()));
                response.statusMessage(status.getDescription());
                response.end();
            }
            listener.onComplete();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<Req, Resp> getMethodDescriptor() {
            return methodDef.getMethodDescriptor();
        }

        @Override
        public void setCompression(String encoding) {
            // ????
            super.setCompression(encoding);
        }

        @Override
        public void setMessageCompression(boolean enabled) {
            // ????
            super.setMessageCompression(enabled);
        }

        @Override
        public Attributes getAttributes() {
            return this.attributes;
        }
    }
}
