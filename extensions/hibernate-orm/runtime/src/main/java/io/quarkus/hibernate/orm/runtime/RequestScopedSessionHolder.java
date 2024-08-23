package io.quarkus.hibernate.orm.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Bean that is used to manage request scoped sessions
 */
@RequestScoped
public class RequestScopedSessionHolder {

    private final Map<String, Session> sessions = new HashMap<>();

    public Session getOrCreateSession(String name, SessionFactory factory) {
        return sessions.computeIfAbsent(name, (n) -> factory.openSession());
    }

    @PreDestroy
    public void destroy() {
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            entry.getValue().close();
        }
    }

}
