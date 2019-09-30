package io.quarkus.arc.test.producer.generic;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;

public class WildcardProducerHierarchyTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producer.class);

    @Test
    public void testWildcardProducers() {
        Function<String, Date> fn = Arc.container().instance(new TypeLiteral<Function<String, Date>>() {}).get();
        Assertions.assertNotNull(fn.apply(String.valueOf(System.currentTimeMillis())));
    }

    @Singleton
    static class Producer {

        @Produces
        @ApplicationScoped
        public AsyncBiFunctionService.WithInner<String, Long, Date> inner() {
            return new InnerImpl();
        }

        @Produces
        @Singleton
        public LocalService<? extends AsyncBiFunctionService.WithInner<String, Long, Date>> localInner(AsyncBiFunctionService.WithInner<String, Long, Date> service) {
            return new LocalService<>(service);
        }

        @Produces
        @ApplicationScoped
        public Function<String, Date> deps(LocalService<? extends AsyncBiFunctionService.WithInner<String, Long, Date>> service) {
            return s -> {
                try {
                    return service.service.apply(s, 0L).toCompletableFuture().get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            };
        }
    }

    static class LocalService<S> {
        S service;

        public LocalService(S service) {
            this.service = service;
        }
    }

    interface AsyncBiFunctionService<K, REQ, RES> extends BiFunction<K, REQ, CompletionStage<RES>>, AutoCloseable {
        interface WithInner<K, REQ, RES> extends AsyncBiFunctionService<K, REQ, RES> {
            Function<K, REQ> key();
            Function<REQ, RES> req();
            Function<RES, String> res();
        }
    }

    static class InnerImpl implements AsyncBiFunctionService.WithInner<String, Long, Date> {
        @Override
        public Function<String, Long> key() {
            return Long::parseLong;
        }

        @Override
        public Function<Long, Date> req() {
            return Date::new;
        }

        @Override
        public Function<Date, String> res() {
            return d -> String.valueOf(d.getTime());
        }

        @Override
        public void close() {
        }

        @Override
        public CompletionStage<Date> apply(String s, Long aLong) {
            return CompletableFuture.completedFuture(req().apply(key().apply(s)));
        }
    }
}
