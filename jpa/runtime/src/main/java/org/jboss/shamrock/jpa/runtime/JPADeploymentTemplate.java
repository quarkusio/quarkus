package org.jboss.shamrock.jpa.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.protean.Hibernate;
import org.hibernate.protean.impl.PersistenceUnitsHolder;
import org.jboss.shamrock.jpa.runtime.cdi.SystemEntityManager;
import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;

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
        System.out.println("List of entities found by Shamrock deployment \n" + entities.toString());
    }

    public void callHibernateFeatureInit() {
        Hibernate.featureInit();
    }

    public void boostrapPu(@ContextObject("bean.container") BeanContainer beanContainer, boolean synthetic) {
        //TODO: we need to take qualifiers into account, at the moment we can only have one EM, but this is probably fine for the PoC
        if (synthetic) {
            beanContainer.instance(EntityManagerFactory.class, new AnnotationLiteral<SystemEntityManager>() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return SystemEntityManager.class;
                }
            }).getProperties();
        } else {
            beanContainer.instance(EntityManagerFactory.class).getProperties();
        }
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
