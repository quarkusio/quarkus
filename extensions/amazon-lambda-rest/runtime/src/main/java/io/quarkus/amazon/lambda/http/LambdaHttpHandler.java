package io.quarkus.amazon.lambda.http;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import org.jboss.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class LambdaHttpHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private static final Logger log = Logger.getLogger("quarkus.amazon.lambda.http");

    private static final Headers errorHeaders = new Headers();

    static {
        errorHeaders.putSingle("Content-Type", "application/json");
    }

    public AwsProxyResponse handleRequest(AwsProxyRequest request, Context context) {
        InetSocketAddress clientAddress = getClientAddressFromRequest(request);

        try {
            return nettyDispatch(clientAddress, request, context);
        } catch (Exception e) {
            log.error("Request Failure", e);
            return new AwsProxyResponse(500, errorHeaders, "{ \"message\": \"Internal Server Error\" }");
        }
    }

    private InetSocketAddress getClientAddressFromRequest(AwsProxyRequest request) {
        if (request.getRequestContext() != null && request.getRequestContext().getIdentity() != null) {
            if (request.getRequestContext().getIdentity().getSourceIp() != null) {
                return new InetSocketAddress(request.getRequestContext().getIdentity().getSourceIp(), 443);
            }
        }
        return null;
    }

    private AwsProxyResponse nettyDispatch(InetSocketAddress clientAddress, AwsProxyRequest request, Context context)
            throws Exception {
        String path = getPathFromRequest(request);
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
                ByteBuf body = Unpooled.wrappedBuffer(Base64.getMimeDecoder().decode(request.getBody()));
                requestContent = new DefaultLastHttpContent(body);
            } else {
                ByteBuf body = Unpooled.copiedBuffer(
                        request.getBody(),
                        StandardCharsets.UTF_8
                ); //TODO: do we need to look at the request encoding?
                requestContent = new DefaultLastHttpContent(body);
            }
        }
        NettyResponseHandler handler = new NettyResponseHandler(request);
        VirtualClientConnection connection = VirtualClientConnection
                .connect(handler, VertxHttpRecorder.VIRTUAL_HTTP, clientAddress);

        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
        try {
            return handler.getFuture().get();
        } finally {
            connection.close();
        }
    }

    private String getPathFromRequest(AwsProxyRequest request) throws UnsupportedEncodingException {
        String path;
        if (hasQueryParameters(request)) {
            path = getPathWithQueryParameters(request.getPath(), request);
        } else {
            path = request.getPath();
        }
        return path;
    }

    private boolean hasQueryParameters(AwsProxyRequest request) {
        return request.getMultiValueQueryStringParameters() != null && !request.getMultiValueQueryStringParameters()
                .isEmpty();
    }

    private String getPathWithQueryParameters(String path,
            AwsProxyRequest request) throws UnsupportedEncodingException {
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
        return sb.toString();
    }
}
