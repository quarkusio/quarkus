package io.quarkus.hibernate.reactive.runtime.transaction;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.reactive.transaction.AfterWorkStrategy;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

@ApplicationScoped
public class AfterWorkTransactionStrategy implements AfterWorkStrategy<Void> {

    private static final Logger LOG = Logger.getLogger(AfterWorkTransactionStrategy.class);

    @Override
    public Uni<Void> getAfterWorkActions(Context context) {
        Uni<Void> voidUni = Uni.combine().all().unis(
                        HibernateReactiveRecorder.OPENED_SESSIONS_STATE.closeAllOpenedSessions(context),
                        HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATELESS.closeAllOpenedSessions(context))
                .discardItems();
        return voidUni.eventually(() -> {
            // We want to make sure that we clear the state after the closing (and after the flushing) as well
            context.removeLocal(TRANSACTIONAL_METHOD_KEY);
        });

    }
}
