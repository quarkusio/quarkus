package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Bean that is used to manage request scoped entity managers
 */
@RequestScoped
public class RequestScopedEntityManagerHolder {

    private final Map<String, EntityManager> entityManagers = new ConcurrentHashMap<>();

    public EntityManager getOrCreateEntityManager(String name, EntityManagerFactory factory) {
        return entityManagers.computeIfAbsent(name, (n) -> factory.createEntityManager());
    }

    @PreDestroy
    public void destroy() {
        for (Map.Entry<String, EntityManager> entry : entityManagers.entrySet()) {
            entry.getValue().close();
        }
    }

}
