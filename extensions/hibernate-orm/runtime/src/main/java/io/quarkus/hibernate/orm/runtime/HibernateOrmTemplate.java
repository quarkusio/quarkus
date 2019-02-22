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

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Template;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Template
public class HibernateOrmTemplate {

    private List<String> entities = new ArrayList<>();

    private static final String CONNECTION_URL = "hibernate.connection.url";

    public void addEntity(String entityClass) {
        entities.add(entityClass);
    }

    public void enlistPersistenceUnit() {
        Logger.getLogger("io.quarkus.hibernate.orm").debugf("List of entities found by Quarkus deployment:%n%s", entities);
    }

    public void callHibernateFeatureInit() {
        Hibernate.featureInit();
    }

    public BeanContainerListener initializeJpa(boolean jtaEnabled) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                beanContainer.instance(JPAConfig.class).setJtaEnabled(jtaEnabled);
            }
        };
    }

    public BeanContainerListener registerPersistenceUnit(String unitName) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                beanContainer.instance(JPAConfig.class).registerPersistenceUnit(unitName);
            }
        };
    }

    public BeanContainerListener initDefaultPersistenceUnit() {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                beanContainer.instance(JPAConfig.class).initDefaultPersistenceUnit();
            }
        };
    }

    public BeanContainerListener initMetadata(List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors,
            Scanner scanner) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                //this initializes the JPA metadata, and also sets the datasource if no connection URL has been set and a DataSource
                //is available
                if (beanContainer != null) {
                    BeanContainer.Factory<DataSource> ds = beanContainer.instanceFactory(DataSource.class);
                    if (ds != null) {
                        DataSource dataSource = ds.create().get();
                        for (ParsedPersistenceXmlDescriptor i : parsedPersistenceXmlDescriptors) {
                            if (!i.getProperties().containsKey(CONNECTION_URL)) {
                                i.setJtaDataSource(dataSource);
                            }
                        }
                    }
                }

                PersistenceUnitsHolder.initializeJpa(parsedPersistenceXmlDescriptors, scanner);
            }
        };
    }

    public void startAllPersistenceUnits(BeanContainer beanContainer) {
        beanContainer.instance(JPAConfig.class).startAll();
    }
}
