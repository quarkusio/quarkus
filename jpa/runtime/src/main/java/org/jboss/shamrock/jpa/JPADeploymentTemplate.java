package org.jboss.shamrock.jpa;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class JPADeploymentTemplate {
    private List<String> entities = new ArrayList<>();

    public void addEntity(String entityClass) {
        entities.add(entityClass);
    }

    public void enlistPersistenceUnit() {
        System.out.println("List of entities \n" + entities.toString());
    }
}
