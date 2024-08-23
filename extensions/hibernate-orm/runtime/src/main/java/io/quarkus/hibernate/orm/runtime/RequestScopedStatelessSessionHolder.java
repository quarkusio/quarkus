package io.quarkus.hibernate.orm.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

/**
 * Bean that is used to manage request scoped stateless sessions
 */
@RequestScoped
public class RequestScopedStatelessSessionHolder {

    private final Map<String, StatelessSession> sessions = new HashMap<>();

    public StatelessSession getOrCreateSession(String name, SessionFactory factory) {
        return sessions.computeIfAbsent(name, (n) -> factory.openStatelessSession());
    }

    @PreDestroy
    public void destroy() {
        for (Map.Entry<String, StatelessSession> entry : sessions.entrySet()) {
            entry.getValue().close();
        }
    }

}
