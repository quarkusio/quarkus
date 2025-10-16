package io.quarkus.hibernate.reactive.runtime;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

public class OpenedSessionsStateStatelessImpl extends OpenedSessionsState<Mutiny.StatelessSession> {
    @Override
    protected Mutiny.StatelessSession newSessionMethod(Mutiny.SessionFactory sessionFactory) {
        return sessionFactory.createStatelessSession();
    }

    @Override
    protected Class<Mutiny.StatelessSession> getSessionType() {
        return Mutiny.StatelessSession.class;
    }

    @Override
    protected boolean isSessionOpen(Mutiny.StatelessSession session) {
        return session.isOpen();
    }

    @Override
    protected Uni<Void> flushSession(Mutiny.StatelessSession session) {
        return Uni.createFrom().voidItem();
    }
}
