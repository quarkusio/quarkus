package org.jboss.shamrock.jpa.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.protean.Hibernate;
import org.hibernate.protean.impl.PersistenceUnitsHolder;
import org.jboss.logging.Logger;
import org.jboss.shamrock.jpa.runtime.cdi.SystemEntityManager;
import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.StartupContext;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class JPADeploymentTemplate {
    private List<String> entities = new ArrayList<>();

    private static final String CONNECTION_URL = "hibernate.connection.url";

    public void addEntity(String entityClass) {
        entities.add(entityClass);
    }

    public void enlistPersistenceUnit() {
        Logger.getLogger("org.jboss.shamrock.jpa").infof("List of entities found by Shamrock deployment:%n%s", entities);
    }

    public void callHibernateFeatureInit() {
        Hibernate.featureInit();
    }

    public void boostrapPu(@ContextObject("bean.container") BeanContainer beanContainer, boolean synthetic, StartupContext startupContext) {
        //TODO: we need to take qualifiers into account, at the moment we can only have one EM, but this is probably fine for the PoC
        final EntityManagerFactory emf;
        if (synthetic) {
            emf = beanContainer.instance(EntityManagerFactory.class, new AnnotationLiteral<SystemEntityManager>() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return SystemEntityManager.class;
                }
            });
            emf.getProperties();
        } else {
            emf = beanContainer.instance(EntityManagerFactory.class);
            emf.getProperties();
        }
        startupContext.addCloseable(new Closeable() {
            @Override
            public void close() throws IOException {
                emf.close();
            }
        });
    }

    public void initMetadata(List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors, Scanner scanner, @ContextObject("bean.container") BeanContainer beanContainer) {

        //this initializes the JPA metadata, and also sets the datasource if no connection URL has been set and a DataSource
        //is available
        if (beanContainer != null) {
            BeanContainer.Factory<DataSource> ds = beanContainer.instanceFactory(DataSource.class);
            if (ds != null) {
                DataSource dataSource = ds.get();
                for (ParsedPersistenceXmlDescriptor i : parsedPersistenceXmlDescriptors) {
                    if (!i.getProperties().containsKey(CONNECTION_URL)) {
                        i.setJtaDataSource(dataSource);
                    }
                }
            }
        }

        PersistenceUnitsHolder.initializeJpa(parsedPersistenceXmlDescriptors, scanner);
    }

}
