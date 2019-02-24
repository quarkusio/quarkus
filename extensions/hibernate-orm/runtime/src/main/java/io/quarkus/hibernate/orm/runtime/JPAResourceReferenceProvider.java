/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
