package io.quarkus.hibernate.reactive.runtime;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

public class OpenedSessionsStateStatefulImpl extends OpenedSessionsState<Mutiny.Session> {

    @Override
    protected Mutiny.Session newSessionMethod(Mutiny.SessionFactory sessionFactory) {
        return sessionFactory.createSession();
    }

    @Override
    protected Class<Mutiny.Session> getSessionType() {
        return Mutiny.Session.class;
    }

    @Override
    protected boolean isSessionOpen(Mutiny.Session session) {
        return session.isOpen();
    }

    @Override
    protected Uni<Void> flushSession(Mutiny.Session session) {
        return session.flush();
    }
}
