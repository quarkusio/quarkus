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

package org.jboss.shamrock.jpa.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;


/**
 * Bean that is used to manage request scoped entity managers
 */
@RequestScoped
public class RequestScopedEntityManagerHolder {

    private final Map<String, EntityManager> entityManagers = new HashMap<>();

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
