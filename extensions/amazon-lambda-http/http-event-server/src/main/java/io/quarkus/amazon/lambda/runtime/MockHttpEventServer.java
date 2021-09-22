package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class MockHttpEventServer extends MockEventServer {

    private final ObjectMapper objectMapper;
    private final ObjectWriter eventWriter;
    private final ObjectReader responseReader;

    public MockHttpEventServer() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        eventWriter = objectMapper.writerFor(APIGatewayV2HTTPEvent.class);
        responseReader = objectMapper.readerFor(APIGatewayV2HTTPResponse.class);
    }

    @Override
    protected void defaultHanderSetup() {
        router.route().handler(this::handleHttpRequests);
    }

    public void handleHttpRequests(RoutingContext ctx) {
        String requestId = ctx.request().getHeader(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        ctx.put(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID, requestId);
        String traceId = ctx.request().getHeader(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        ctx.put(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY, traceId);
        Buffer body = ctx.getBody();

        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        event.getRequestContext().setHttp(new APIGatewayV2HTTPEvent.RequestContext.Http());
        event.getRequestContext().getHttp().setMethod(ctx.request().method().name());
        event.setRawPath(ctx.request().path());
        event.setRawQueryString(ctx.request().query());
        for (String header : ctx.request().headers().names()) {
            if (event.getHeaders() == null)
                event.setHeaders(new HashMap<>());
            List<String> values = ctx.request().headers().getAll(header);
            String value = String.join(",", values);
            event.getHeaders().put(header, value);
        }
        if (body != null) {
            String ct = ctx.request().getHeader("content-type");
            if (ct == null || isBinary(ct)) {
                String encoded = Base64.getMimeEncoder().encodeToString(body.getBytes());
                event.setBody(encoded);
                event.setIsBase64Encoded(true);
            } else {
                event.setBody(new String(body.getBytes(), StandardCharsets.UTF_8));
            }
        }

        try {
            byte[] mEvent = eventWriter.writeValueAsBytes(event);
            ctx.put(APIGatewayV2HTTPEvent.class.getName(), mEvent);
            log.debugf("Putting message %s into the queue", requestId);
            queue.put(ctx);
        } catch (Exception e) {
            log.error("Publish failure", e);
            ctx.fail(500);
        }
    }

    @Override
    protected String getEventContentType(RoutingContext request) {
        if (request.get(APIGatewayV2HTTPEvent.class.getName()) != null)
            return "application/json";
        return super.getEventContentType(request);
    }

    @Override
    protected Buffer processEventBody(RoutingContext request) {
        byte[] buf = request.get(APIGatewayV2HTTPEvent.class.getName());
        if (buf != null) {
            return Buffer.buffer(buf);
        }
        return super.processEventBody(request);
    }

    @Override
    public void processResponse(RoutingContext ctx, RoutingContext pending, Buffer buffer) {
        if (pending.get(APIGatewayV2HTTPEvent.class.getName()) != null) {
            try {
                APIGatewayV2HTTPResponse res = responseReader.readValue(buffer.getBytes());
                HttpServerResponse response = pending.response();
                if (res.getHeaders() != null) {
                    for (Map.Entry<String, String> header : res.getHeaders().entrySet()) {
                        for (String val : header.getValue().split(",")) {
                            response.headers().add(header.getKey(), val);
                        }
                    }
                }
                response.setStatusCode(res.getStatusCode());
                String body = res.getBody();
                if (body != null) {
                    if (res.getIsBase64Encoded()) {
                        byte[] bytes = Base64.getDecoder().decode(body);
                        response.end(Buffer.buffer(bytes));
                    } else {
                        response.end(body);
                    }
                } else {
                    response.end();
                }

            } catch (IOException e) {
                log.error("Publish failure", e);
                pending.fail(500);
            }
        } else {
            super.processResponse(ctx, pending, buffer);
        }
    }

    private boolean isBinary(String contentType) {
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            return !(ct.startsWith("text") || ct.contains("json") || ct.contains("xml") || ct.contains("yaml"));
        }
        return false;
    }

}
