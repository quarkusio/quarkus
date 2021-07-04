package io.quarkus.amazon.lambda.http;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

@SuppressWarnings("unused")
public class LambdaHttpHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final Logger log = Logger.getLogger("quarkus.amazon.lambda.http");

    private static final Headers ERROR_HEADERS = new Headers();

    static {
        ERROR_HEADERS.putSingle("Content-Type", "application/json");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        InetSocketAddress clientAddress = getClientAddress(request);

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

    private InetSocketAddress getClientAddress(APIGatewayV2HTTPEvent request) {
        if (request.getRequestContext() != null && request.getRequestContext().getHttp() != null) {
            if (request.getRequestContext().getHttp().getSourceIp() != null) {
                return new InetSocketAddress(request.getRequestContext().getHttp().getSourceIp(), 443);
            }
        }
        return null;
    }

    private APIGatewayV2HTTPResponse nettyDispatch(InetSocketAddress clientAddress, APIGatewayV2HTTPEvent request,
            Context context)
            throws Exception {
        QuarkusHttpHeaders quarkusHeaders = new QuarkusHttpHeaders();
        quarkusHeaders.setContextObject(Context.class, context);
        quarkusHeaders.setContextObject(APIGatewayV2HTTPEvent.class, request);
        quarkusHeaders.setContextObject(APIGatewayV2HTTPEvent.RequestContext.class, request.getRequestContext());

        NettyResponseHandler handler = new NettyResponseHandler(request);
        VirtualClientConnection connection = VirtualClientConnection.connect(handler, VertxHttpRecorder.VIRTUAL_HTTP,
                clientAddress);

        connection.sendMessage(createHttpRequestHead(request, quarkusHeaders));
        connection.sendMessage(createHttpRequestBody(request));
        try {
            return handler.getFuture().get();
        } finally {
            connection.close();
        }
    }

    private HttpRequest createHttpRequestHead(APIGatewayV2HTTPEvent request, QuarkusHttpHeaders quarkusHeaders) {
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(
                HttpVersion.HTTP_1_1,
                extractHttpMethodFromRequest(request),
                extractHttpUrlFromRequest(request),
                quarkusHeaders);

        HttpHeaders nettyRequestHeaders = nettyRequest.headers();
        if (request.getHeaders() != null) { //apparently this can be null if no headers are sent
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                if (header.getValue() != null) {
                    for (String val : header.getValue().split(","))
                        nettyRequestHeaders.add(header.getKey(), val);
                }
            }
        }
        if (!nettyRequestHeaders.contains(HttpHeaderNames.HOST)) {
            nettyRequestHeaders.add(HttpHeaderNames.HOST, "localhost");
        }
        return nettyRequest;
    }

    private HttpMethod extractHttpMethodFromRequest(APIGatewayV2HTTPEvent request) {
        return HttpMethod.valueOf(request.getRequestContext().getHttp().getMethod());
    }

    private String extractHttpUrlFromRequest(APIGatewayV2HTTPEvent request) {
        return Optional.ofNullable(request.getRawQueryString())
                .filter(q -> !q.isEmpty())
                .map(q -> request.getRawPath() + '?' + q)
                .orElse(request.getRawPath());
    }

    private HttpContent createHttpRequestBody(APIGatewayV2HTTPEvent request) {
        if (request.getBody() != null) {
            ByteBuf body;
            if (request.getIsBase64Encoded()) {
                body = Unpooled.wrappedBuffer(Base64.getMimeDecoder().decode(request.getBody()));
            } else {
                //TODO: do we need to look at the request encoding?
                body = Unpooled.copiedBuffer(request.getBody(), StandardCharsets.UTF_8);
            }
            return new DefaultLastHttpContent(body);
        } else {
            return LastHttpContent.EMPTY_LAST_CONTENT;
        }
    }
}
