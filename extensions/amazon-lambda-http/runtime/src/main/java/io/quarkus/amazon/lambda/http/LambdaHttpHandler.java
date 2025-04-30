package io.quarkus.amazon.lambda.http;

import static java.util.Optional.ofNullable;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

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
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.ConnectionBase;

@SuppressWarnings("unused")
public class LambdaHttpHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final Logger log = Logger.getLogger("quarkus.amazon.lambda.http");

    private static final int BUFFER_SIZE = 8096;

    private static final Map<String, List<String>> ERROR_HEADERS = Map.of("Content-Type", List.of("application/json"));

    private static final String COOKIE_HEADER = "Cookie";

    // comma headers for headers that have comma in value and we don't want to split it up into
    // multiple headers
    private static final Set<String> COMMA_HEADERS = Set.of("access-control-request-headers");

    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        InetSocketAddress clientAddress = null;
        if (request.getRequestContext() != null && request.getRequestContext().getHttp() != null) {
            if (request.getRequestContext().getHttp().getSourceIp() != null) {
                clientAddress = new InetSocketAddress(request.getRequestContext().getHttp().getSourceIp(), 443);
            }
        }

        try {
            return nettyDispatch(clientAddress, request, context);
        } catch (Exception e) {
            log.error("Request Failure", e);
            APIGatewayV2HTTPResponse res = new APIGatewayV2HTTPResponse();
            res.setStatusCode(500);
            res.setBody("{ \"message\": \"Internal Server Error\" }");
            res.setMultiValueHeaders(ERROR_HEADERS);
            return res;
        }

    }

    private class NettyResponseHandler implements VirtualResponseHandler {
        APIGatewayV2HTTPResponse responseBuilder = new APIGatewayV2HTTPResponse();
        ByteArrayOutputStream baos;
        WritableByteChannel byteChannel;
        final APIGatewayV2HTTPEvent request;
        CompletableFuture<APIGatewayV2HTTPResponse> future = new CompletableFuture<>();

        public NettyResponseHandler(APIGatewayV2HTTPEvent request) {
            this.request = request;
        }

        public CompletableFuture<APIGatewayV2HTTPResponse> getFuture() {
            return future;
        }

        @Override
        public void handleMessage(Object msg) {
            try {
                //log.info("Got message: " + msg.getClass().getName());

                if (msg instanceof HttpResponse) {
                    HttpResponse res = (HttpResponse) msg;
                    responseBuilder.setStatusCode(res.status().code());

                    final Map<String, String> headers = new HashMap<>();
                    responseBuilder.setHeaders(headers);
                    for (String name : res.headers().names()) {
                        final List<String> allForName = res.headers().getAll(name);
                        if (allForName == null || allForName.isEmpty()) {
                            continue;
                        }
                        // Handle cookies separately to preserve commas in the header values
                        if ("set-cookie".equals(name)) {
                            responseBuilder.setCookies(allForName);
                            continue;
                        }
                        final StringBuilder sb = new StringBuilder();
                        for (Iterator<String> valueIterator = allForName.iterator(); valueIterator.hasNext();) {
                            String val = valueIterator.next();
                            if (name.equalsIgnoreCase("Transfer-Encoding")
                                    && val.equals("chunked")) {
                                continue; // ignore transfer encoding, chunked screws up message and response
                            }
                            sb.append(val);
                            if (valueIterator.hasNext()) {
                                sb.append(",");
                            }
                        }
                        headers.put(name, sb.toString());
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
                        if (isText(responseBuilder.getHeaders().get("Content-Type"))) {
                            responseBuilder.setBody(baos.toString(StandardCharsets.UTF_8));
                        } else {
                            responseBuilder.setIsBase64Encoded(true);
                            responseBuilder.setBody(Base64.getEncoder().encodeToString(baos.toByteArray()));
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

    private APIGatewayV2HTTPResponse nettyDispatch(InetSocketAddress clientAddress, APIGatewayV2HTTPEvent request,
            Context context)
            throws Exception {
        QuarkusHttpHeaders quarkusHeaders = new QuarkusHttpHeaders();
        quarkusHeaders.setContextObject(Context.class, context);
        quarkusHeaders.setContextObject(APIGatewayV2HTTPEvent.class, request);
        quarkusHeaders.setContextObject(APIGatewayV2HTTPEvent.RequestContext.class, request.getRequestContext());
        HttpMethod httpMethod = null;
        if (request.getRequestContext() != null && request.getRequestContext().getHttp() != null
                && request.getRequestContext().getHttp().getMethod() != null) {
            httpMethod = HttpMethod.valueOf(request.getRequestContext().getHttp().getMethod());
        }
        if (httpMethod == null) {
            throw new IllegalStateException("Missing HTTP method in request event");
        }
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                httpMethod, ofNullable(request.getRawQueryString())
                        .filter(q -> !q.isEmpty()).map(q -> request.getRawPath() + '?' + q).orElse(request.getRawPath()),
                quarkusHeaders);
        if (request.getHeaders() != null) { //apparently this can be null if no headers are sent
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                if (header.getValue() != null) {
                    // Some header values have commas in them and we don't want to
                    // split them up into multiple header values.
                    if (COMMA_HEADERS.contains(header.getKey().toLowerCase(Locale.ROOT))) {
                        nettyRequest.headers().add(header.getKey(), header.getValue());
                    } else {
                        for (String val : header.getValue().split(","))
                            nettyRequest.headers().add(header.getKey(), val);
                    }
                }
            }
        }
        if (request.getCookies() != null) {
            nettyRequest.headers().add(COOKIE_HEADER, String.join("; ", request.getCookies()));
        }

        if (!nettyRequest.headers().contains(HttpHeaderNames.HOST)) {
            nettyRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        }

        HttpContent requestContent = LastHttpContent.EMPTY_LAST_CONTENT;
        if (request.getBody() != null) {
            // See https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.3
            nettyRequest.headers().add(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
            if (request.getIsBase64Encoded()) {
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
        if (request.getRequestContext() != null
                && request.getRequestContext().getHttp() != null
                && request.getRequestContext().getHttp().getSourceIp() != null
                && request.getRequestContext().getHttp().getSourceIp().length() > 0) {
            int port = 443; // todo, may be bad to assume 443?
            if (request.getHeaders() != null &&
                    request.getHeaders().get("X-Forwarded-Port") != null) {
                port = Integer.parseInt(request.getHeaders().get("X-Forwarded-Port"));
            }
            connection.peer().attr(ConnectionBase.REMOTE_ADDRESS_OVERRIDE).set(
                    SocketAddress.inetSocketAddress(port,
                            request.getRequestContext().getHttp().getSourceIp()));
        }
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

    private boolean isText(String contentType) {
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            return (ct.startsWith("text") || ct.contains("json") || (ct.contains("xml") && !ct.contains("openxmlformats"))
                    || ct.contains("yaml"));
        }
        return false;
    }

}
