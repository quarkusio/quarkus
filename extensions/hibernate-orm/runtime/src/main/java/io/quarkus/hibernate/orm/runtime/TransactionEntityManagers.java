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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import io.quarkus.hibernate.orm.runtime.entitymanager.TransactionScopedEntityManager;

public class TransactionEntityManagers {

    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager tm;

    @Inject
    JPAConfig jpaConfig;

    @Inject
    Instance<RequestScopedEntityManagerHolder> requestScopedEntityManagers;

    private final Map<String, TransactionScopedEntityManager> managers;

    public TransactionEntityManagers() {
        this.managers = new HashMap<>();
    }

    public EntityManager getEntityManager(String unitName) {
        return managers.computeIfAbsent(unitName,
                un -> new TransactionScopedEntityManager(tm, tsr, jpaConfig.getEntityManagerFactory(un), unitName,
                        requestScopedEntityManagers));
    }

}
