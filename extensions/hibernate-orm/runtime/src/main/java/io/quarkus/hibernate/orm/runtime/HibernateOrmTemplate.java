package io.quarkus.hibernate.orm.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.service.spi.ServiceContributor;
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
            Scanner scanner, Collection<Class<? extends Integrator>> additionalIntegrators,
            Collection<Class<? extends ServiceContributor>> additionalServiceContributors) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                PersistenceUnitsHolder.initializeJpa(parsedPersistenceXmlDescriptors, scanner, additionalIntegrators,
                        additionalServiceContributors);
            }
        };
    }

    public void startAllPersistenceUnits(BeanContainer beanContainer) {
        beanContainer.instance(JPAConfig.class).startAll();
    }
}
