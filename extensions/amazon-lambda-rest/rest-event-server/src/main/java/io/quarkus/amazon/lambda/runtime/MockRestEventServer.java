package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.http.model.MultiValuedTreeMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class MockRestEventServer extends MockEventServer {

    private final ObjectMapper objectMapper;
    private final ObjectWriter eventWriter;
    private final ObjectReader responseReader;

    public MockRestEventServer() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        eventWriter = objectMapper.writerFor(AwsProxyRequest.class);
        responseReader = objectMapper.readerFor(AwsProxyResponse.class);
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

        AwsProxyRequest event = new AwsProxyRequest();
        event.setRequestContext(new AwsProxyRequestContext());
        event.getRequestContext().setRequestId(requestId);
        event.getRequestContext().setHttpMethod(ctx.request().method().name());
        event.setHttpMethod(ctx.request().method().name());
        event.setPath(ctx.request().path());
        if (ctx.request().query() != null) {
            event.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
            String[] params = ctx.request().query().split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String[] keyval = param.split("=");
                    try {
                        event.getMultiValueQueryStringParameters().add(
                                URLDecoder.decode(keyval[0], StandardCharsets.UTF_8.name()),
                                URLDecoder.decode(keyval[1], StandardCharsets.UTF_8.name()));
                    } catch (UnsupportedEncodingException e) {
                        log.error("Failed to parse query string", e);
                        ctx.response().setStatusCode(400).end();
                        return;
                    }
                }
            }

        }
        if (ctx.request().headers() != null) {
            event.setMultiValueHeaders(new Headers());
            for (String header : ctx.request().headers().names()) {
                List<String> values = ctx.request().headers().getAll(header);
                for (String val : values)
                    event.getMultiValueHeaders().add(header, val);
            }
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
            ctx.put(AwsProxyRequest.class.getName(), mEvent);
            log.debugf("Putting message %s into the queue", requestId);
            queue.put(ctx);
        } catch (Exception e) {
            log.error("Publish failure", e);
            ctx.fail(500);
        }
    }

    @Override
    protected String getEventContentType(RoutingContext request) {
        if (request.get(AwsProxyRequest.class.getName()) != null)
            return "application/json";
        return super.getEventContentType(request);
    }

    @Override
    protected Buffer processEventBody(RoutingContext request) {
        byte[] buf = request.get(AwsProxyRequest.class.getName());
        if (buf != null) {
            return Buffer.buffer(buf);
        }
        return super.processEventBody(request);
    }

    @Override
    public void processResponse(RoutingContext ctx, RoutingContext pending, Buffer buffer) {
        if (pending.get(AwsProxyRequest.class.getName()) != null) {
            try {
                AwsProxyResponse res = responseReader.readValue(buffer.getBytes());
                HttpServerResponse response = pending.response();
                if (res.getMultiValueHeaders() != null) {
                    for (Map.Entry<String, List<String>> header : res.getMultiValueHeaders().entrySet()) {
                        for (String val : header.getValue()) {
                            response.headers().add(header.getKey(), val);
                        }
                    }
                }
                response.setStatusCode(res.getStatusCode());
                String body = res.getBody();
                if (body != null) {
                    if (res.isBase64Encoded()) {
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
