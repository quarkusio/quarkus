package io.quarkus.hibernate.reactive.runtime.transaction;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.reactive.transaction.AfterWorkStrategy;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@ApplicationScoped
public class AfterWorkTransactionStrategy implements AfterWorkStrategy<Void> {
    @Override
    public Uni<Void> getAfterWorkActions(Context context) {
        return Uni.combine().all().unis(
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE.closeAllOpenedSessions(context),
                HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATELESS.closeAllOpenedSessions(context))
                .discardItems();
    }
}
