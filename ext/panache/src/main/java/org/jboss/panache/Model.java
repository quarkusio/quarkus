package org.jboss.panache;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.jboss.protean.arc.Arc;

@MappedSuperclass
public class Model {
    
    @Id
    @GeneratedValue
    public Integer id;
    
    // Operations
    
    public void save() {
    	EntityManager em = getEntityManager();
        if (!em.contains(this)) {
        	System.err.println("persist");
            em.persist(this);
        }
        em.flush();
    }
    
    public void delete() {
    	EntityManager em = getEntityManager();
    	em.remove(this);
    	em.flush();
    }
    
    public boolean isPersistent() {
    	return getEntityManager().contains(this);
    }

    private static EntityManager getEntityManager() {
        return Arc.container().instance(EntityManager.class).get();
    }

    // Queries
    
    public static <T extends Model> T findById(Integer id) {
    	throw new RuntimeException("Should never be called");
    }

    protected static <T extends Model> T findById(Class<T> entityClass, Integer id) {
    	return getEntityManager().find(entityClass, id);
    }

	public static <T extends Model> List<T> findAll() {
    	throw new RuntimeException("Should never be called");
	}

	protected static <T extends Model> List<T> findAll(Class<T> entityClass) {
    	return getEntityManager().createQuery("FROM "+entityClass.getName()).getResultList();
	}
}
