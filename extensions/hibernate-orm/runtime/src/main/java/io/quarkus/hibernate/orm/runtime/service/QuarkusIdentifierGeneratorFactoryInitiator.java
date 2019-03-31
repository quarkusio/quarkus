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
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

public final class QuarkusIdentifierGeneratorFactoryInitiator
        implements StandardServiceInitiator<MutableIdentifierGeneratorFactory> {

    private final Dialect dialect;
    private final Map<String, IdentifierGenerator> strategyToIdGeneratorMap;

    public QuarkusIdentifierGeneratorFactoryInitiator(Dialect dialect,
            Map<String, IdentifierGenerator> strategyToIdGeneratorMap) {
        this.dialect = dialect;
        this.strategyToIdGeneratorMap = strategyToIdGeneratorMap;
    }

    @Override
    public Class<MutableIdentifierGeneratorFactory> getServiceInitiated() {
        return MutableIdentifierGeneratorFactory.class;
    }

    @Override
    public MutableIdentifierGeneratorFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusIdentifierGeneratorFactory(dialect, strategyToIdGeneratorMap);
    }

    public static class QuarkusIdentifierGeneratorFactory implements MutableIdentifierGeneratorFactory {

        private final Dialect dialect;
        private final Map<String, IdentifierGenerator> strategyToIdGeneratorMap;

        public QuarkusIdentifierGeneratorFactory(Dialect dialect, Map<String, IdentifierGenerator> strategyToIdGeneratorMap) {
            this.dialect = dialect;
            this.strategyToIdGeneratorMap = strategyToIdGeneratorMap;
        }

        @Override
        public Dialect getDialect() {
            return dialect;
        }

        @Override
        public void setDialect(Dialect dialect) {

        }

        @Override
        public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config) {
            ensureProperStrategy(strategy);
            return strategyToIdGeneratorMap.get(strategy);
        }

        @Override
        public Class getIdentifierGeneratorClass(String strategy) {
            ensureProperStrategy(strategy);
            return strategyToIdGeneratorMap.get(strategy).getClass();
        }

        private void ensureProperStrategy(String strategy) {
            if (strategyToIdGeneratorMap.containsKey(strategy)) {
                throw new IllegalArgumentException("IdentifierGenerator " + strategy
                        + " was not captured during augmentation and therefore cannot be used at runtime");
            }
        }

        @Override
        public void register(String strategy, Class generatorClass) {
            throw new IllegalStateException("This should never be called at this point");
        }
    }

}
