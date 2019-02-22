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

package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.service.spi.ServiceRegistryAwareService;

public final class RecordingDialectFactory extends DialectFactoryImpl
        implements DialectFactory, ServiceRegistryAwareService {

    private Dialect dialect;

    @Override
    public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource)
            throws HibernateException {
        dialect = super.buildDialect(configValues, resolutionInfoSource);
        return dialect;
    }

    public Dialect getDialect() {
        return dialect;
    }

}
