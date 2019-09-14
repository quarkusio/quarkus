package io.quarkus.hibernate.orm.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ResourceReferenceProvider;
import io.quarkus.hibernate.orm.runtime.entitymanager.ForwardingEntityManager;

public class JPAResourceReferenceProvider implements ResourceReferenceProvider {
    private static final Map<String, InstanceHandle<Object>> entityManagers = new ConcurrentHashMap<>();
    private static final Map<String, InstanceHandle<Object>> entityManagerFactories = new ConcurrentHashMap<>();

    @Override
    public InstanceHandle<Object> get(Type type, Set<Annotation> annotations) {
        JPAConfig jpaConfig = Arc.container().instance(JPAConfig.class).get();
        if (jpaConfig == null) {
            return null;
        }

        if (EntityManagerFactory.class.equals(type)) {
            PersistenceUnit pu = getAnnotation(annotations, PersistenceUnit.class);
            if (pu != null) {
                return getEntityManagerFactory(jpaConfig, pu.unitName());
            }
        } else if (EntityManager.class.equals(type)) {
            PersistenceContext pc = getAnnotation(annotations, PersistenceContext.class);
            if (pc != null) {
                final String unitName = pc.unitName();
                if (jpaConfig.isJtaEnabled()) {
                    return getTransactionEntityManager(unitName);
                } else {
                    return getEntityManager(jpaConfig, unitName);
                }
            }
        }

        return null;
    }

    private InstanceHandle<Object> getEntityManagerFactory(JPAConfig jpaConfig, String unitName) {
        InstanceHandle<Object> instanceHandle = entityManagerFactories.get(unitName);
        if (instanceHandle != null) {
            return instanceHandle;
        }

        EntityManagerFactory entityManagerFactory = jpaConfig.getEntityManagerFactory(unitName);

        instanceHandle = new InstanceHandle<Object>() {
            @Override
            public Object get() {
                return entityManagerFactory;
            }
        };

        entityManagerFactories.put(unitName, instanceHandle);
        return instanceHandle;
    }

    private InstanceHandle<Object> getTransactionEntityManager(String unitName) {
        TransactionEntityManagers transactionEntityManagers = Arc.container()
                .instance(TransactionEntityManagers.class).get();
        ForwardingEntityManager entityManager = new ForwardingEntityManager() {
            @Override
            protected EntityManager delegate() {
                return transactionEntityManagers.getEntityManager(unitName);
            }
        };

        return new InstanceHandle<Object>() {
            @Override
            public Object get() {
                return entityManager;
            }
        };
    }

    private InstanceHandle<Object> getEntityManager(JPAConfig jpaConfig, String unitName) {
        InstanceHandle<Object> entityManagerInstanceHandle = entityManagers.get(unitName);

        if (entityManagerInstanceHandle != null) {
            return entityManagerInstanceHandle;
        }

        InstanceHandle<Object> entityManagerFactoryInstanceHandle = getEntityManagerFactory(jpaConfig, unitName);

        EntityManager entityManager = EntityManagerFactory.class.cast(entityManagerFactoryInstanceHandle.get())
                .createEntityManager();

        InstanceHandle<Object> instanceHandle = new InstanceHandle<Object>() {
            @Override
            public Object get() {
                return entityManager;
            }

            @Override
            public void destroy() {
                entityManager.close();
            }
        };

        entityManagers.put(unitName, instanceHandle);
        return instanceHandle;
    }

}
