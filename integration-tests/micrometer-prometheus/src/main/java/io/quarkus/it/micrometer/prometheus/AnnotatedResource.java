package io.quarkus.it.micrometer.prometheus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;

@Path("/all-the-things")
public class AnnotatedResource {
    private static final Logger log = Logger.getLogger(AnnotatedResource.class);

    @GET
    public String allTheThings() {
        // Counted
        onlyCountFailures();
        countAllInvocations(false);
        emptyMetricName(false);
        wrap(x -> countAllInvocations(true));
        wrap(x -> emptyMetricName(true));

        join(x -> onlyCountAsyncFailures());
        join(x -> countAllAsyncInvocations(false));
        join(x -> emptyAsyncMetricName(false));
        join(x -> countAllAsyncInvocations(true));
        join(x -> emptyAsyncMetricName(true));

        //Timed
        call(false);
        longCall(false);
        wrap(x -> call(true));
        wrap(x -> longCall(true));

        join(x -> asyncCall(false));
        join(x -> longAsyncCall(false));
        join(x -> asyncCall(true));
        join(x -> longAsyncCall(true));

        return "OK";
    }

    void wrap(Function<Boolean, Object> function) {
        try {
            function.apply(true);
        } catch (NullPointerException e) {
            if (!e.getMessage().equals("Failed on purpose")) {
                log.error("Unexpected exception in test", e);
                throw e;
            }
        }
    }

    void join(Function<Boolean, CompletableFuture<?>> function) {
        try {
            function.apply(true).join();
        } catch (CompletionException e) {
            if (!e.getCause().getMessage().equals("Failed on purpose")) {
                log.error("unexpected exception in test", e);
                throw e;
            }
        }
    }

    @Counted(value = "metric.none", recordFailuresOnly = true)
    public Object onlyCountFailures() {
        return new Response(false).get();
    }

    @Counted(value = "metric.all", extraTags = { "extra", "tag" })
    public Object countAllInvocations(boolean fail) {
        return new Response(fail).get();
    }

    @Counted(description = "nice description")
    public Object emptyMetricName(boolean fail) {
        return new Response(fail).get();
    }

    @Counted(value = "async.none", recordFailuresOnly = true)
    public CompletableFuture<?> onlyCountAsyncFailures() {
        return CompletableFuture.supplyAsync(new Response(false));
    }

    @Counted(value = "async.all", extraTags = { "extra", "tag" })
    public CompletableFuture<?> countAllAsyncInvocations(boolean fail) {
        return CompletableFuture.supplyAsync(new Response(fail));
    }

    @Counted
    public CompletableFuture<?> emptyAsyncMetricName(boolean fail) {
        return CompletableFuture.supplyAsync(new Response(fail));
    }

    @Timed(value = "call", extraTags = { "extra", "tag" })
    public Object call(boolean fail) {
        return new Response(fail).get();
    }

    @Timed(value = "longCall", extraTags = { "extra", "tag" }, longTask = true)
    public Object longCall(boolean fail) {
        return new Response(fail).get();
    }

    @Timed(value = "async.call", extraTags = { "extra", "tag" })
    public CompletableFuture<?> asyncCall(boolean fail) {
        return CompletableFuture.supplyAsync(new Response(fail));
    }

    @Timed(value = "async.longCall", extraTags = { "extra", "tag" }, longTask = true)
    public CompletableFuture<?> longAsyncCall(boolean fail) {
        return CompletableFuture.supplyAsync(new Response(fail));
    }

    static class Response implements Supplier<Object> {
        boolean fail;

        Response(boolean fail) {
            this.fail = fail;
        }

        @Override
        public Object get() {
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                // intentionally empty
            }
            if (fail) {
                throw new NullPointerException("Failed on purpose");
            }
            return new Object();
        }
    }
}
