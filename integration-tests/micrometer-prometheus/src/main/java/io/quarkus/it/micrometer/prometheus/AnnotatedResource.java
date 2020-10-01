package io.quarkus.it.micrometer.prometheus;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;

@Path("/all-the-things")
public class AnnotatedResource {

    @GET
    public String allTheThings() {
        // Counted
        onlyCountFailures();
        countAllInvocations(false);
        emptyMetricName(false);
        onlyCountAsyncFailures();
        countAllAsyncInvocations(false);
        emptyAsyncMetricName(false);

        wrap(x -> countAllInvocations(true));
        wrap(x -> emptyMetricName(true));
        wrap(x -> countAllAsyncInvocations(true));
        wrap(x -> emptyAsyncMetricName(true));

        //Timed
        call(false);
        asyncCall(false);
        longCall(false);
        longAsyncCall(false);

        wrap(x -> call(true));
        wrap(x -> asyncCall(true));
        wrap(x -> longCall(true));
        wrap(x -> longAsyncCall(true));

        return "OK";
    }

    void wrap(Function function) {
        try {
            function.apply(true);
        } catch (RuntimeException e) {
            if (!e.getMessage().equals("Failed on purpose")) {
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
                throw new RuntimeException("Failed on purpose");
            }
            return new Object();
        }
    }
}
