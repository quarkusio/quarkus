package io.quarkus.hibernate.orm.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ResourceReferenceProvider;
import io.quarkus.hibernate.orm.runtime.entitymanager.ForwardingEntityManager;

public class JPAResourceReferenceProvider implements ResourceReferenceProvider {

    @Override
    public InstanceHandle<Object> get(Type type, Set<Annotation> annotations) {
        JPAConfig jpaConfig = Arc.container().instance(JPAConfig.class).get();

        if (EntityManagerFactory.class.equals(type)) {
            PersistenceUnit pu = getAnnotation(annotations, PersistenceUnit.class);
            if (pu != null) {
                return () -> jpaConfig.getEntityManagerFactory(pu.unitName());
            }
        } else if (EntityManager.class.equals(type)) {
            PersistenceContext pc = getAnnotation(annotations, PersistenceContext.class);
            if (pc != null) {
                if (jpaConfig.isJtaEnabled()) {
                    TransactionEntityManagers transactionEntityManagers = Arc.container()
                            .instance(TransactionEntityManagers.class).get();
                    ForwardingEntityManager entityManager = new ForwardingEntityManager() {

                        @Override
                        protected EntityManager delegate() {
                            return transactionEntityManagers.getEntityManager(pc.unitName());
                        }
                    };
                    return () -> entityManager;
                } else {
                    EntityManagerFactory entityManagerFactory = jpaConfig.getEntityManagerFactory(pc.unitName());
                    EntityManager entityManager = entityManagerFactory.createEntityManager();
                    return new InstanceHandle<Object>() {

                        @Override
                        public Object get() {
                            return entityManager;
                        }

                        @Override
                        public void destroy() {
                            entityManager.close();
                        }
                    };
                }
            }
        }

        return null;
    }

}
