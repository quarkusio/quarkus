package io.quarkus.amazon.lambda.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

@Deprecated
public class LambdaClient {
    protected static final Logger log = Logger.getLogger(LambdaClient.class);

    private static final AtomicInteger REQUEST_ID_GENERATOR = new AtomicInteger();
    public static final ConcurrentHashMap<String, CompletableFuture<String>> REQUESTS;
    public static final LinkedBlockingDeque<Map.Entry<String, String>> REQUEST_QUEUE;
    static volatile LambdaException problem;

    static {
        //a hack around class loading
        //this is always loaded in the root class loader with jboss-logmanager,
        //however it may also be loaded in an isolated CL when running in dev
        //or test mode. If it is in an isolated CL we load the handler from
        //the class on the system class loader so they are equal
        //TODO: should this class go in its own module and be excluded from isolated class loading?
        ConcurrentHashMap<String, CompletableFuture<String>> requests = new ConcurrentHashMap<>();
        LinkedBlockingDeque<Map.Entry<String, String>> requestQueue = new LinkedBlockingDeque<>();
        ClassLoader cl = LambdaClient.class.getClassLoader();
        try {
            Class<?> root = Class.forName(LambdaClient.class.getName(), false, ClassLoader.getSystemClassLoader());
            if (root.getClassLoader() != cl) {
                requestQueue = (LinkedBlockingDeque<Map.Entry<String, String>>) root.getDeclaredField("REQUEST_QUEUE")
                        .get(null);
                requests = (ConcurrentHashMap<String, CompletableFuture<String>>) root.getDeclaredField("REQUESTS").get(null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        REQUESTS = requests;
        REQUEST_QUEUE = requestQueue;

    }

    @Deprecated
    public static void invoke(InputStream inputStream, OutputStream outputStream) {
        log.warn(
                "LambdaClient has been deprecated and will be removed in future Quarkus versions.  You can now invoke using a built in test http server.  See docs for more details");
        if (problem != null) {
            throw new RuntimeException(problem);
        }
        try {
            String id = "aws-request-" + REQUEST_ID_GENERATOR.incrementAndGet();
            CompletableFuture<String> result = new CompletableFuture<>();
            REQUESTS.put(id, result);
            StringBuilder requestBody = new StringBuilder();
            int i = 0;
            while ((i = inputStream.read()) != -1) {
                requestBody.append((char) i);
            }
            REQUEST_QUEUE.add(new Map.Entry<String, String>() {

                @Override
                public String getKey() {
                    return id;
                }

                @Override
                public String getValue() {
                    return requestBody.toString();
                }

                @Override
                public String setValue(String value) {
                    return null;
                }
            });
            String output = result.get();
            outputStream.write(output.getBytes());
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

    /**
     * Marshalls input and output as JSON using Jackson
     *
     * @param returnType
     * @param input
     * @param <T>
     * @return
     */
    @Deprecated
    public static <T> T invoke(Class<T> returnType, Object input) {
        return invoke(returnType, input, Duration.ofNanos(Long.MAX_VALUE));
    }

    @Deprecated
    public static <T> T invoke(Class<T> returnType, Object input, Duration timeout) {
        try {
            return invokeAsync(returnType, input).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static <T> CompletableFuture<T> invokeAsync(Class<T> returnType, Object input) {
        log.warn(
                "LambdaClient has been deprecated and will be removed in future Quarkus versions.  You can now invoke using a built in test http server.  See docs for more details");
        if (problem != null) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(problem);
            return failed;
        }
        final ObjectMapper mapper = getObjectMapper();
        final String id = "aws-request-" + REQUEST_ID_GENERATOR.incrementAndGet();
        final String requestBody;
        final CompletableFuture<String> result = new CompletableFuture<>();
        REQUESTS.put(id, result);
        try {
            requestBody = mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        REQUEST_QUEUE.add(new AbstractMap.SimpleImmutableEntry(id, requestBody));
        return result.thenApply(s -> {
            try {
                return mapper.readerFor(returnType).readValue(s);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Takes a json string as input. Unmarshalls return value using Jackson.
     *
     * @param returnType
     * @param json
     * @param <T>
     * @return
     */
    @Deprecated
    public static <T> T invokeJson(Class<T> returnType, String json) {
        log.warn(
                "LambdaClient has been deprecated and will be removed in future Quarkus versions.  You can now invoke using a built in test http server.  See docs for more details");
        if (problem != null) {
            throw new RuntimeException(problem);
        }
        try {
            final ObjectMapper mapper = getObjectMapper();
            String id = "aws-request-" + REQUEST_ID_GENERATOR.incrementAndGet();
            CompletableFuture<String> result = new CompletableFuture<>();
            REQUESTS.put(id, result);
            REQUEST_QUEUE.add(new Map.Entry<String, String>() {

                @Override
                public String getKey() {
                    return id;
                }

                @Override
                public String getValue() {
                    return json;
                }

                @Override
                public String setValue(String value) {
                    return null;
                }
            });
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

    private static ObjectMapper getObjectMapper() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return new ObjectMapper();
        }
        InstanceHandle<ObjectMapper> instance = container.instance(ObjectMapper.class);
        if (instance.isAvailable()) {
            return instance.get();
        }
        return new ObjectMapper();
    }

}
