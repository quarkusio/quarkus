package io.quarkus.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

@Singleton
public class LoggingBean implements LoggingInterface {
    // not final to prevent constant inlining
    private static String msg = "Heya!";

    static {
        Log.info(msg);
    }

    @PostConstruct
    public void setup() {
        Log.tracef("%s created", LoggingBean.class.getSimpleName());
    }

    public void doSomething() {
        inheritedMethod("abc");

        if (Log.isDebugEnabled()) {
            Log.debug("starting massive computation");
        }

        Log.debugf("one: %d", 42);
        Log.tracef("two: %d | %d", 42, 13);
        Log.debugf("three: %d | %d | %d", 42, 13, 1);

        Log.debugv("one: {0}", "foo");
        Log.infov("two: {0} | {1}", "foo", "bar");
        Log.warnv("three: {0} | {1} | {2}", "foo", "bar", "baz");
        Log.errorv("four: {0} | {1} | {2} | {3}", "foo", "bar", "baz", "quux");

        Exception error = new NoStackTraceTestException();

        Log.warnv(error, "{0} | {1} | {2} | {3}", "foo", "bar", "baz", "quux");

        Log.error("Hello Error", error);
    }

    // https://github.com/quarkusio/quarkus/issues/32663
    public void reproduceStackDisciplineIssue() {
        String result;
        String now = "now";

        Log.infov("{0} {1}", "number", 42);
        Log.info("string " + now);
    }

    // https://quarkusio.zulipchat.com/#narrow/channel/187030-users/topic/Using.20logging.2ELog.20only.20possible.20with.20bytecode.20transform
    public void reproduceMethodReferenceIssue() {
        Stream.of("foo", "bar", "baz", "quux").forEach(Log::info);
        BiStream.of("foo", new NoStackTraceTestException(), "bar", new NoStackTraceTestException())
                .when(Log::isDebugEnabled)
                .forEach(Log::debug);
        BiStream.of("foo %s", "bar", "baz %s", "quux").forEach(Log::warnf);
        TriStream.of("foo %s %s", "bar", "baz").forEach(Log::infof);
        TetraStream.of("foo %s %s %s", "bar", "baz", "quux").forEach(Log::errorf);
        PentaStream.of("foo %s %s %s %s", "bar", "baz", "qux", "quux").forEach(Log::infof);
        HexaStream.of("foo %s %s %s %s %s", "bar", "baz", "qux", "quux", "quuux").forEach(Log::infof);
    }

    static class BiStream<T, U> {
        private record Item<T, U>(T t, U u) {
        }

        private final List<Item<T, U>> list;

        static <T, U> BiStream<T, U> of(T t1, U u1, T t2, U u2) {
            List<Item<T, U>> list = new ArrayList<>();
            list.add(new Item<>(t1, u1));
            list.add(new Item<>(t2, u2));
            return new BiStream<>(list);
        }

        private BiStream(List<Item<T, U>> list) {
            this.list = list;
        }

        BiStream<T, U> when(BooleanSupplier filter) {
            if (filter.getAsBoolean()) {
                return this;
            }
            return new BiStream<>(List.of());
        }

        void forEach(BiConsumer<T, U> action) {
            list.forEach(item -> action.accept(item.t(), item.u()));
        }
    }

    @FunctionalInterface
    interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    static class TriStream<T, U, V> {
        private record Item<T, U, V>(T t, U u, V v) {
        }

        private final List<Item<T, U, V>> list;

        static <T, U, V> TriStream<T, U, V> of(T t1, U u1, V v1) {
            List<Item<T, U, V>> list = new ArrayList<>();
            list.add(new Item<>(t1, u1, v1));
            return new TriStream<>(list);
        }

        private TriStream(List<Item<T, U, V>> list) {
            this.list = list;
        }

        void forEach(TriConsumer<T, U, V> action) {
            list.forEach(item -> action.accept(item.t(), item.u(), item.v()));
        }
    }

    @FunctionalInterface
    interface TetraConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);
    }

    static class TetraStream<T, U, V, W> {
        private record Item<T, U, V, W>(T t, U u, V v, W w) {
        }

        private final List<Item<T, U, V, W>> list;

        static <T, U, V, W> TetraStream<T, U, V, W> of(T t1, U u1, V v1, W w1) {
            List<Item<T, U, V, W>> list = new ArrayList<>();
            list.add(new Item<>(t1, u1, v1, w1));
            return new TetraStream<>(list);
        }

        private TetraStream(List<Item<T, U, V, W>> list) {
            this.list = list;
        }

        void forEach(TetraConsumer<T, U, V, W> action) {
            list.forEach(item -> action.accept(item.t(), item.u(), item.v(), item.w()));
        }
    }

    @FunctionalInterface
    interface PentaConsumer<T, U, V, W, X> {
        void accept(T t, U u, V v, W w, X x);
    }

    static class PentaStream<T, U, V, W, X> {
        private record Item<T, U, V, W, X>(T t, U u, V v, W w, X x) {
        }

        private final List<Item<T, U, V, W, X>> list;

        static <T, U, V, W, X> PentaStream<T, U, V, W, X> of(T t1, U u1, V v1, W w1, X x1) {
            List<Item<T, U, V, W, X>> list = new ArrayList<>();
            list.add(new Item<>(t1, u1, v1, w1, x1));
            return new PentaStream<>(list);
        }

        private PentaStream(List<Item<T, U, V, W, X>> list) {
            this.list = list;
        }

        void forEach(PentaConsumer<T, U, V, W, X> action) {
            list.forEach(item -> action.accept(item.t(), item.u(), item.v(), item.w(), item.x()));
        }
    }

    @FunctionalInterface
    interface HexaConsumer<T, U, V, W, X, Y> {
        void accept(T t, U u, V v, W w, X x, Y y);
    }

    static class HexaStream<T, U, V, W, X, Y> {
        private record Item<T, U, V, W, X, Y>(T t, U u, V v, W w, X x, Y y) {
        }

        private final List<Item<T, U, V, W, X, Y>> list;

        static <T, U, V, W, X, Y> HexaStream<T, U, V, W, X, Y> of(T t1, U u1, V v1, W w1, X x1, Y y1) {
            List<Item<T, U, V, W, X, Y>> list = new ArrayList<>();
            list.add(new Item<>(t1, u1, v1, w1, x1, y1));
            return new HexaStream<>(list);
        }

        private HexaStream(List<Item<T, U, V, W, X, Y>> list) {
            this.list = list;
        }

        void forEach(HexaConsumer<T, U, V, W, X, Y> action) {
            list.forEach(item -> action.accept(item.t(), item.u(), item.v(), item.w(), item.x(), item.y()));
        }
    }
}
