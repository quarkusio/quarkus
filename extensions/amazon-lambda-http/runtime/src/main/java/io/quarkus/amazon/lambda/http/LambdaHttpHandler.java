package io.quarkus.amazon.lambda.http;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

@SuppressWarnings("unused")
public class LambdaHttpHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {

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

        VirtualClientConnection connection = VirtualClientConnection.connect(VertxHttpRecorder.VIRTUAL_HTTP, clientAddress);
        try {
            return nettyDispatch(connection, request);
        } catch (Exception e) {
            return new AwsProxyResponse(500, errorHeaders, "{ \"message\": \"Internal Server Error\" }");
        } finally {
            connection.close();
        }

    }

    private AwsProxyResponse nettyDispatch(VirtualClientConnection connection, AwsProxyRequest request) throws Exception {
        String path = request.getPath();
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
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getHttpMethod()), path);
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
                ByteBuf body = Unpooled.wrappedBuffer(Base64.getMimeDecoder().decode(request.getBody()));
                requestContent = new DefaultLastHttpContent(body);
            } else {
                ByteBuf body = Unpooled.copiedBuffer(request.getBody(), StandardCharsets.UTF_8); //TODO: do we need to look at the request encoding?
                requestContent = new DefaultLastHttpContent(body);
            }
        }

        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
        AwsProxyResponse responseBuilder = new AwsProxyResponse();
        ByteArrayOutputStream baos = null;
        for (;;) {
            // todo should we timeout? have a timeout config?
            //log.info("waiting for message");
            Object msg = connection.queue().poll(100, TimeUnit.MILLISECONDS);
            try {
                if (msg == null)
                    continue;
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
                        // todo what is right size?
                        baos = new ByteArrayOutputStream(500);
                    }
                    for (int i = 0; i < readable; i++) {
                        baos.write(content.content().readByte());
                    }
                }
                if (msg instanceof LastHttpContent) {
                    if (baos != null) {
                        if (isBinary(responseBuilder.getMultiValueHeaders().getFirst("Content-Type"))) {
                            responseBuilder.setBase64Encoded(true);
                            responseBuilder.setBody(Base64.getMimeEncoder().encodeToString(baos.toByteArray()));
                        } else {
                            responseBuilder.setBody(new String(baos.toByteArray(), "UTF-8"));
                        }
                    }
                    return responseBuilder;
                }
            } finally {
                if (msg != null)
                    ReferenceCountUtil.release(msg);
            }
        }
    }

    private boolean isBinary(String contentType) {
        if (contentType != null) {
            int index = contentType.indexOf(';');
            if (index >= 0) {
                return LambdaContainerHandler.getContainerConfig().isBinaryContentType(contentType.substring(0, index));
            } else {
                return LambdaContainerHandler.getContainerConfig().isBinaryContentType(contentType);
            }
        }
        return false;
    }

}
