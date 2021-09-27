package io.quarkus.amazon.lambda.http;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

@SuppressWarnings("unused")
public class LambdaHttpHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private static final Logger log = Logger.getLogger("quarkus.amazon.lambda.http");

    private static final int BUFFER_SIZE = 8096;

    private static Headers errorHeaders = new Headers();
    static {
        errorHeaders.putSingle("Content-Type", "application/json");
    }

    public AwsProxyResponse handleRequest(AwsProxyRequest request, Context context) {
        InetSocketAddress clientAddress = null;
        if (request.getRequestContext() != null && request.getRequestContext().getIdentity() != null) {
            if (request.getRequestContext().getIdentity().getSourceIp() != null) {
                clientAddress = new InetSocketAddress(request.getRequestContext().getIdentity().getSourceIp(), 443);
            }
        }

        try {
            return nettyDispatch(clientAddress, request, context);
        } catch (Exception e) {
            log.error("Request Failure", e);
            return new AwsProxyResponse(500, errorHeaders, "{ \"message\": \"Internal Server Error\" }");
        }

    }

    private class NettyResponseHandler implements VirtualResponseHandler {
        AwsProxyResponse responseBuilder = new AwsProxyResponse();
        ByteArrayOutputStream baos;
        WritableByteChannel byteChannel;
        final AwsProxyRequest request;
        CompletableFuture<AwsProxyResponse> future = new CompletableFuture<>();

        public NettyResponseHandler(AwsProxyRequest request) {
            this.request = request;
        }

        public CompletableFuture<AwsProxyResponse> getFuture() {
            return future;
        }

        @Override
        public void handleMessage(Object msg) {
            try {
                //log.info("Got message: " + msg.getClass().getName());

                if (msg instanceof HttpResponse) {
                    HttpResponse res = (HttpResponse) msg;
                    responseBuilder.setStatusCode(res.status().code());

                    if (request.getRequestSource() == AwsProxyRequest.RequestSource.ALB) {
                        responseBuilder.setStatusDescription(res.status().reasonPhrase());
                    }
                    responseBuilder.setMultiValueHeaders(new Headers());
                    for (String name : res.headers().names()) {
                        for (String v : res.headers().getAll(name)) {
                            responseBuilder.getMultiValueHeaders().add(name, v);
                        }
                    }
                }
                if (msg instanceof HttpContent) {
                    HttpContent content = (HttpContent) msg;
                    int readable = content.content().readableBytes();
                    if (baos == null && readable > 0) {
                        baos = createByteStream();
                    }
                    for (int i = 0; i < readable; i++) {
                        baos.write(content.content().readByte());
                    }
                }
                if (msg instanceof FileRegion) {
                    FileRegion file = (FileRegion) msg;
                    if (file.count() > 0 && file.transferred() < file.count()) {
                        if (baos == null)
                            baos = createByteStream();
                        if (byteChannel == null)
                            byteChannel = Channels.newChannel(baos);
                        file.transferTo(byteChannel, file.transferred());
                    }
                }
                if (msg instanceof LastHttpContent) {
                    if (baos != null) {
                        if (isBinary(responseBuilder.getMultiValueHeaders().getFirst("Content-Type"))) {
                            responseBuilder.setBase64Encoded(true);
                            responseBuilder.setBody(Base64.getEncoder().encodeToString(baos.toByteArray()));
                        } else {
                            responseBuilder.setBody(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                        }
                    }
                    future.complete(responseBuilder);
                }
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            } finally {
                if (msg != null) {
                    ReferenceCountUtil.release(msg);
                }
            }
        }

        @Override
        public void close() {
            if (!future.isDone())
                future.completeExceptionally(new RuntimeException("Connection closed"));
        }
    }

    private AwsProxyResponse nettyDispatch(InetSocketAddress clientAddress, AwsProxyRequest request, Context context)
            throws Exception {
        String path = request.getPath();
        //log.info("---- Got lambda request: " + path);
        if (request.getMultiValueQueryStringParameters() != null && !request.getMultiValueQueryStringParameters().isEmpty()) {
            StringBuilder sb = new StringBuilder(path);
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, List<String>> e : request.getMultiValueQueryStringParameters().entrySet()) {
                for (String v : e.getValue()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append("&");
                    }
                    if (request.getRequestSource() == AwsProxyRequest.RequestSource.ALB) {
                        sb.append(e.getKey());
                        sb.append("=");
                        sb.append(v);
                    } else {
                        sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.name()));
                        sb.append("=");
                        sb.append(URLEncoder.encode(v, StandardCharsets.UTF_8.name()));
                    }
                }
            }
            path = sb.toString();
        }
        QuarkusHttpHeaders quarkusHeaders = new QuarkusHttpHeaders();
        quarkusHeaders.setContextObject(Context.class, context);
        quarkusHeaders.setContextObject(AwsProxyRequestContext.class, request.getRequestContext());
        quarkusHeaders.setContextObject(AwsProxyRequest.class, request);
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getHttpMethod()), path, quarkusHeaders);
        if (request.getMultiValueHeaders() != null) { //apparently this can be null if no headers are sent
            for (Map.Entry<String, List<String>> header : request.getMultiValueHeaders().entrySet()) {
                nettyRequest.headers().add(header.getKey(), header.getValue());
            }
        }
        if (!nettyRequest.headers().contains(HttpHeaderNames.HOST)) {
            nettyRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        }

        HttpContent requestContent = LastHttpContent.EMPTY_LAST_CONTENT;
        if (request.getBody() != null) {
            if (request.isBase64Encoded()) {
                ByteBuf body = Unpooled.wrappedBuffer(Base64.getDecoder().decode(request.getBody()));
                requestContent = new DefaultLastHttpContent(body);
            } else {
                ByteBuf body = Unpooled.copiedBuffer(request.getBody(), StandardCharsets.UTF_8); //TODO: do we need to look at the request encoding?
                requestContent = new DefaultLastHttpContent(body);
            }
        }
        NettyResponseHandler handler = new NettyResponseHandler(request);
        VirtualClientConnection connection = VirtualClientConnection.connect(handler, VertxHttpRecorder.VIRTUAL_HTTP,
                clientAddress);

        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
        try {
            return handler.getFuture().get();
        } finally {
            connection.close();
        }
    }

    private ByteArrayOutputStream createByteStream() {
        ByteArrayOutputStream baos;
        baos = new ByteArrayOutputStream(BUFFER_SIZE);
        return baos;
    }

    private boolean isBinary(String contentType) {
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            return !(ct.startsWith("text") || ct.contains("json") || ct.contains("xml") || ct.contains("yaml"));
        }
        return false;
    }
}
