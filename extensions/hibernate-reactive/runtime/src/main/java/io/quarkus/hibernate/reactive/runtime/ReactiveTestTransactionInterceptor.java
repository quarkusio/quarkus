package io.quarkus.hibernate.reactive.runtime;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import io.quarkus.reactive.transaction.runtime.ReactiveResource;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase;
import io.quarkus.reactive.transaction.runtime.pool.TransactionalContextPool;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

// Registered as an interceptor bean by HibernateReactiveProcessor.reactiveTestTx() only during tests
public class ReactiveTestTransactionInterceptor {

    @Inject
    public ReactiveResource reactiveResource;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        if (!ic.getMethod().getReturnType().getName().equals(Uni.class.getName())) {
            return ic.proceed();
        }

        return Uni.createFrom().deferred(() -> {
            Context vertxContext = Vertx.currentContext();
            ContextLocals.put(TRANSACTIONAL_METHOD_KEY, true);

            return TransactionalInterceptorBase.proceedUni(ic)
                    .call(() -> reactiveResource.beforeCommit(vertxContext))
                    .onTermination().call(() -> rollback())
                    .eventually(() -> reactiveResource.afterCommit(vertxContext))
                    .eventually(() -> closeConnection());
        });
    }

    private Uni<Void> rollback() {
        Future<? extends SqlConnection> future = TransactionalContextPool.getCurrentConnectionFromVertxContext();
        if (future == null) {
            return Uni.createFrom().voidItem();
        }
        return toUni(future).onItem().transformToUni(connection -> {
            Transaction transaction = connection.transaction();
            if (transaction == null) {
                return Uni.createFrom().voidItem();
            }
            return toUni(transaction.rollback());
        });
    }

    private Uni<Void> closeConnection() {
        Future<Void> closeFuture = TransactionalContextPool.closeAndClearCurrentConnection();
        if (closeFuture == null) {
            return Uni.createFrom().voidItem();
        }
        return toUni(closeFuture);
    }

    private static <T> Uni<T> toUni(Future<? extends T> future) {
        return Uni.createFrom()
                .emitter(emitter -> future.onComplete(emitter::complete, emitter::fail));
    }
}
