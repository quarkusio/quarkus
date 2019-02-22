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

package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJdbcEnvironmentInitiator implements StandardServiceInitiator<JdbcEnvironment> {

    private final Dialect dialect;

    public QuarkusJdbcEnvironmentInitiator(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Class<JdbcEnvironment> getServiceInitiated() {
        return JdbcEnvironment.class;
    }

    @Override
    public JdbcEnvironment initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new JdbcEnvironmentImpl(registry, dialect);
    }

}