package io.quarkus.test.vertx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

/**
 * A subclass can be used to wrap the injected {@link UniAsserter} and customize the default behavior.
 * <p>
 * Specifically, it can intercept selected methods and perform some additional logic. The
 * {@link #transformUni(Supplier)} method can be used to transform the provided {@link Uni} supplier for assertion and
 * {@link UniAsserter#execute(Supplier)} methods.
 * <p>
 * For example, it can be used to perform all assertions within the scope of a database transaction:
 *
 * <pre>
 * &#64;QuarkusTest
 * public class SomeTest {
 *
 *     static class TransactionalUniAsserterInterceptor extends UniAsserterInterceptor {
 *
 *         public TransactionUniAsserterInterceptor(UniAsserter asserter){
 *         super(asserter);
 *         }
 *
 *         &#64;Override
 *         protected &lt;T&gt; Supplier&lt;Uni&lt;T&gt;&gt; transformUni(Supplier&lt;Uni&lt;T&gt;&gt; uniSupplier) {
 *             // Assert/execute methods are invoked within a database transaction
 *             return () -> Panache.withTransaction(uniSupplier);
 *         }
 *     }
 *
 *     &#64;Test
 *     &#64;RunOnVertxContext
 *     public void testEntity(UniAsserter asserter) {
 *         asserter = new TransactionalUniAsserterInterceptor(asserter);
 *         asserter.execute(() -> new MyEntity().persist());
 *         asserter.assertEquals(() -&gt; MyEntity.count(), 1l);
 *         asserter.execute(() -> MyEntity.deleteAll());
 *     }
 * }
 * </pre>
 *
 * Alternatively, an anonymous class can be used as well:
 *
 * <pre>
 * &#64;QuarkusTest
 * public class SomeTest {
 *
 *     &#64;Test
 *     &#64;RunOnVertxContext
 *     public void testEntity(UniAsserter asserter) {
 *         asserter = new UniAsserterInterceptor(asserter) {
 *             &#64;Override
 *             protected &lt;T&gt; Supplier&lt;Uni&lt;T&gt;&gt; transformUni(Supplier&lt;Uni&lt;T&gt;&gt; uniSupplier) {
 *                 return () -&gt; Panache.withTransaction(uniSupplier);
 *             }
 *         };
 *         asserter.execute(() -&gt; new MyEntity().persist());
 *         asserter.assertEquals(() -&gt; MyEntity.count(), 1l);
 *         asserter.execute(() -&gt; MyEntity.deleteAll());
 *     }
 * }
 * </pre>
 */
public abstract class UniAsserterInterceptor implements UnwrappableUniAsserter {

    private final UniAsserter delegate;

    public UniAsserterInterceptor(UniAsserter asserter) {
        this.delegate = asserter;
    }

    /**
     * This method can be used to transform the provided {@link Uni} supplier for assertion methods and
     * {@link UniAsserter#execute(Supplier)}.
     *
     * @param <T>
     * @param uniSupplier
     *
     * @return
     */
    protected <T> Supplier<Uni<T>> transformUni(Supplier<Uni<T>> uniSupplier) {
        return uniSupplier;
    }

    @Override
    public <T> UniAsserter assertThat(Supplier<Uni<T>> uni, Consumer<T> asserter) {
        delegate.assertThat(transformUni(uni), asserter);
        return this;
    }

    @Override
    public <T> UniAsserter execute(Supplier<Uni<T>> uni) {
        delegate.execute(transformUni(uni));
        return this;
    }

    @Override
    public UniAsserter execute(Runnable c) {
        delegate.execute(c);
        return this;
    }

    @Override
    public <T> UniAsserter assertEquals(Supplier<Uni<T>> uni, T t) {
        delegate.assertEquals(transformUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertNotEquals(Supplier<Uni<T>> uni, T t) {
        delegate.assertNotEquals(transformUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertSame(Supplier<Uni<T>> uni, T t) {
        delegate.assertSame(transformUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertNotSame(Supplier<Uni<T>> uni, T t) {
        delegate.assertNotSame(transformUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertNull(Supplier<Uni<T>> uni) {
        delegate.assertNull(transformUni(uni));
        return this;
    }

    @Override
    public <T> UniAsserter assertNotNull(Supplier<Uni<T>> uni) {
        delegate.assertNotNull(transformUni(uni));
        return this;
    }

    @Override
    public <T> UniAsserter surroundWith(Function<Uni<T>, Uni<T>> uni) {
        delegate.surroundWith(uni);
        return this;
    }

    @Override
    public UniAsserter fail() {
        delegate.fail();
        return this;
    }

    @Override
    public UniAsserter assertTrue(Supplier<Uni<Boolean>> uni) {
        delegate.assertTrue(transformUni(uni));
        return this;
    }

    @Override
    public UniAsserter assertFalse(Supplier<Uni<Boolean>> uni) {
        delegate.assertFalse(transformUni(uni));
        return this;
    }

    @Override
    public <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Consumer<Throwable> c) {
        delegate.assertFailedWith(transformUni(uni), c);
        return this;
    }

    @Override
    public <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Class<? extends Throwable> c) {
        delegate.assertFailedWith(transformUni(uni), c);
        return this;
    }

    @Override
    public Object getData(String key) {
        return delegate.getData(key);
    }

    @Override
    public Object putData(String key, Object value) {
        return delegate.putData(key, value);
    }

    @Override
    public void clearData() {
        delegate.clearData();
    }

    @Override
    public Uni<?> asUni() {
        return ((UnwrappableUniAsserter) delegate).asUni();
    }
}
