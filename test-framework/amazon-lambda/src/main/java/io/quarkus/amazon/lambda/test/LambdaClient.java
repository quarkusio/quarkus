package io.quarkus.amazon.lambda.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LambdaClient {

    private static final AtomicInteger REQUEST_ID_GENERATOR = new AtomicInteger();
    static final ConcurrentHashMap<String, CompletableFuture<String>> REQUESTS = new ConcurrentHashMap<>();
    static final LinkedBlockingDeque<Request> REQUEST_QUEUE = new LinkedBlockingDeque<>();
    static volatile LambdaException problem;

    public static <T> T invoke(Class<T> returnType, Object input) {
        if (problem != null) {
            throw new RuntimeException(problem);
        }
        try {
            final ObjectMapper mapper = new ObjectMapper();
            String id = "aws-request-" + REQUEST_ID_GENERATOR.incrementAndGet();
            CompletableFuture<String> result = new CompletableFuture<>();
            REQUESTS.put(id, result);
            REQUEST_QUEUE.add(new Request(id, mapper.writeValueAsString(input)));
            String output = result.get();
            return mapper.readerFor(returnType).readValue(output);
        } catch (Exception e) {
            if (e instanceof ExecutionException) {
                Throwable ex = e.getCause();
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    public static class Request {
        final String id;
        final String json;

        Request(String id, String json) {
            this.id = id;
            this.json = json;
        }

        public String getId() {
            return id;
        }

        public String getJson() {
            return json;
        }
    }

}
