package org.jboss.shamrock.jpa.runtime;

import org.hibernate.protean.Hibernate;
import org.jboss.shamrock.jpa.runtime.cdi.SystemEntityManager;
import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManagerFactory;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class JPADeploymentTemplate {
    private List<String> entities = new ArrayList<>();

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
        if(synthetic) {
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

}
