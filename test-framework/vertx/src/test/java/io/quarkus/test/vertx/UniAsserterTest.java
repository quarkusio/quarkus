package io.quarkus.test.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import io.smallrye.mutiny.Uni;

public class UniAsserterTest {

    @Test
    public void testAssertEquals() {
        testAsserter(ua -> ua.assertEquals(() -> Uni.createFrom().item("foo"), "foo")
                .assertEquals(() -> Uni.createFrom().item(true), true), true);
        testAsserterFailure(ua -> ua.assertEquals(() -> Uni.createFrom().item(false), true));
    }

    @Test
    public void testAssertNotEquals() {
        testAsserter(ua -> ua.assertNotEquals(() -> Uni.createFrom().item("foo"), "bar"), "foo");
        testAsserterFailure(ua -> ua.assertNotEquals(() -> Uni.createFrom().item(true), true));
    }

    @Test
    public void testAssertTrue() {
        testAsserter(ua -> ua.assertTrue(() -> Uni.createFrom().item(true)));
        testAsserterFailure(ua -> ua.assertTrue(() -> Uni.createFrom().item(false)));
    }

    @Test
    public void testAssertFalse() {
        testAsserter(ua -> ua.assertFalse(() -> Uni.createFrom().item(false)));
        testAsserterFailure(ua -> ua.assertFalse(() -> Uni.createFrom().item(true)));
    }

    @Test
    public void testAssertNull() {
        testAsserter(ua -> ua.assertNull(() -> Uni.createFrom().nullItem()));
        testAsserterFailure(ua -> ua.assertNull(() -> Uni.createFrom().item(false)));
    }

    @Test
    public void testAssertNotNull() {
        testAsserter(ua -> ua.assertNotNull(() -> Uni.createFrom().item(false)));
        testAsserterFailure(ua -> ua.assertNotNull(() -> Uni.createFrom().nullItem()));
    }

    @Test
    public void testAssertFailedWith() {
        testAsserter(ua -> ua.assertFailedWith(() -> Uni.createFrom().failure(new NullPointerException()),
                NullPointerException.class));
        testAsserterFailure(ua -> ua.assertFailedWith(() -> Uni.createFrom().failure(new IllegalStateException()),
                NullPointerException.class), t -> AssertionError.class.isInstance(t));

        // Note that assertFailedWith() is not tested at all because of the exception thrown from the previous
        // assertEquals()
        testAsserterFailure(
                ua -> ua.assertEquals(() -> Uni.createFrom().item("foo"), null).assertFailedWith(
                        () -> Uni.createFrom().failure(new NullPointerException()), IllegalArgumentException.class),
                t -> AssertionError.class.isInstance(t));

        testAsserterFailure(ua -> ua.assertTrue(() -> {
            throw new IllegalArgumentException();
        }).assertFailedWith(() -> Uni.createFrom().failure(new NullPointerException()), IllegalArgumentException.class),
                t -> IllegalArgumentException.class.isInstance(t));
    }

    @Test
    public void testAssertSame() {
        String foo = "foo";
        testAsserter(ua -> ua.assertSame(() -> Uni.createFrom().item(foo), foo));
        testAsserterFailure(ua -> ua.assertSame(() -> Uni.createFrom().item(foo), "bar"));
    }

    @Test
    public void testAssertNotSame() {
        testAsserter(ua -> ua.assertNotSame(() -> Uni.createFrom().item("foo"), new String("foo")));
        testAsserterFailure(ua -> ua.assertNotSame(() -> Uni.createFrom().item("foo"), "foo"));
    }

    @Test
    public void testAssertThat() {
        testAsserter(ua -> ua.assertThat(() -> Uni.createFrom().item("foo"), foo -> assertEquals("foo", foo)));
        testAsserterFailure(ua -> ua.assertThat(() -> Uni.createFrom().item("foo"), foo -> assertEquals("bar", foo)));
    }

    @Test
    public void testFail() {
        testAsserterFailure(ua -> ua.fail(), t -> AssertionFailedError.class.isInstance(t));
    }

    @Test
    public void testExecute() throws InterruptedException, ExecutionException {
        CompletableFuture<Object> cf = new CompletableFuture<>();
        AtomicInteger executeExecuted = new AtomicInteger(0);

        DefaultUniAsserter asserter = new DefaultUniAsserter();
        asserter.assertEquals(() -> Uni.createFrom().item("foo"), "foo");
        asserter.execute(() -> executeExecuted.incrementAndGet());
        asserter.execute(() -> executeExecuted.incrementAndGet());
        asserter.execution.subscribe().with(r -> cf.complete(r), t -> cf.completeExceptionally(t));
        assertEquals("foo", cf.get());
        assertEquals(2, executeExecuted.get());
    }

    @Test
    public void testComplexAssert() {
        testAsserter(ua -> ua.assertThat(() -> Uni.createFrom().item("foo"), foo -> assertEquals("foo", foo))
                .assertEquals(() -> Uni.createFrom().item("foo"), "foo")
                .assertNotEquals(() -> Uni.createFrom().item("foo"), "bar")
                .assertTrue(() -> Uni.createFrom().item(true)).assertNotNull(() -> Uni.createFrom().item("bar")));
    }

    @Test
    public void testData() {
        testAsserter(ua -> ua.assertEquals(() -> {
            ua.putData("foo", "baz");
            return Uni.createFrom().item("foo");
        }, "foo").assertNotEquals(() -> {
            assertEquals("baz", ua.getData("foo"));
            return Uni.createFrom().item("foo");
        }, "bar").assertNotNull(() -> {
            return Uni.createFrom().item(ua.getData("foo"));
        }));

        testAsserter(ua -> ua.assertEquals(() -> {
            ua.putData("foo", "baz");
            ua.putData("bar", true);
            return Uni.createFrom().item("foo");
        }, "foo").assertNotEquals(() -> {
            assertEquals("baz", ua.getData("foo"));
            assertEquals(true, ua.getData("bar"));
            return Uni.createFrom().item("foo");
        }, "bar").assertNotNull(() -> {
            return Uni.createFrom().item(ua.getData("foo"));
        }).assertTrue(() -> {
            return Uni.createFrom().item((Boolean) ua.getData("bar"));
        }));

        testAsserter(ua -> ua.assertEquals(() -> {
            ua.putData("bar", true);
            return Uni.createFrom().item("foo");
        }, "foo").assertNotEquals(() -> {
            assertEquals(true, ua.getData("bar"));
            return Uni.createFrom().item("foo");
        }, "bar").assertNotNull(() -> {
            ua.clearData();
            return Uni.createFrom().item(1);
        }).assertNull(() -> {
            return Uni.createFrom().item(ua.getData("bar"));
        }));
    }

    @Test
    public void testInterceptorFailures() {
        // UniAsserter should fail even though the supplier returns a non-null value
        testAsserterFailure(ua -> {
            UniAsserter asserter = new AlwaysFailingUniAsserterInterceptor(ua);
            asserter.assertNotNull(() -> Uni.createFrom().item(Boolean.TRUE));
        }, t -> IllegalStateException.class.isInstance(t));
    }

    @Test
    public void testInterceptorData() {
        testAsserter(ua -> {
            UniAsserter asserter = new UniAsserterInterceptor(ua) {
                @Override
                public Object getData(String key) {
                    return "found";
                }
            };
            asserter.assertEquals(() -> Uni.createFrom().item(asserter.getData("foo")), "found");
        });
    }

    static class AlwaysFailingUniAsserterInterceptor extends UniAsserterInterceptor {

        public AlwaysFailingUniAsserterInterceptor(UniAsserter asserter) {
            super(asserter);
        }

        @Override
        protected <T> Supplier<Uni<T>> transformUni(Supplier<Uni<T>> uniSupplier) {
            return () -> Uni.createFrom().failure(new IllegalStateException());
        }

    }
    // utils

    private <T> void testAsserter(Consumer<UniAsserter> assertion) {
        testAsserter(assertion, null);
    }

    private <T> void testAsserter(Consumer<UniAsserter> assertion, T finalItem) {
        CompletableFuture<Object> cf = new CompletableFuture<>();
        DefaultUniAsserter asserter = new DefaultUniAsserter();
        assertion.accept(asserter);
        asserter.execution.subscribe().with(r -> cf.complete(r), t -> cf.completeExceptionally(t));
        try {
            Object item = cf.get();
            if (finalItem != null) {
                assertEquals(finalItem, item);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void testAsserterFailure(Consumer<UniAsserter> assertion) {
        testAsserterFailure(assertion, null);
    }

    private void testAsserterFailure(Consumer<UniAsserter> assertion, Predicate<Throwable> expected) {
        CompletableFuture<Object> cf = new CompletableFuture<>();
        DefaultUniAsserter asserter = new DefaultUniAsserter();
        assertion.accept(asserter);
        asserter.execution.subscribe().with(r -> cf.complete(r), t -> cf.completeExceptionally(t));
        try {
            cf.get();
            fail("No failure");
        } catch (ExecutionException e) {
            if (expected != null) {
                assertTrue(expected.test(e.getCause()), "Unexpected exception thrown: " + e.getCause());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
