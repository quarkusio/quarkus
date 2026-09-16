package io.quarkus.amazon.lambda.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;

public class LambdaHttpHandlerTest {

    private static final String LOGGER_NAME = "quarkus.amazon.lambda.http";

    /**
     * An API Gateway HTTP API (payload format 2.0) event, or a non-proxy integration event, deserializes into a proxy
     * request without {@code httpMethod}: the failure must say what is wrong instead of a bare NullPointerException.
     */
    @Test
    public void missingHttpMethodIsReported() {
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        org.jboss.logmanager.Logger jbossLogger = org.jboss.logmanager.Logger.getLogger(LOGGER_NAME);
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(LOGGER_NAME);
        jbossLogger.addHandler(handler);
        julLogger.addHandler(handler);
        try {
            AwsProxyResponse response = new LambdaHttpHandler().handleRequest(new AwsProxyRequest(), null);

            assertEquals(500, response.getStatusCode());
            LogRecord failure = records.stream().filter(r -> r.getThrown() != null).findFirst()
                    .orElseThrow(() -> new AssertionError("The request failure was not logged"));
            assertInstanceOf(IllegalStateException.class, failure.getThrown(), failure.getThrown().toString());
            String message = failure.getThrown().getMessage();
            assertTrue(message.contains("Missing HTTP method"), message);
            assertTrue(message.contains("quarkus-amazon-lambda-http"), message);
        } finally {
            jbossLogger.removeHandler(handler);
            julLogger.removeHandler(handler);
        }
    }
}
