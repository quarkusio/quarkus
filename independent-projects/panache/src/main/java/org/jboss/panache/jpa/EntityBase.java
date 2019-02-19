package org.jboss.panache.jpa;

public class EntityBase {

    // Operations
    
    public void save() {
        JpaOperations.save(this);
    }
    
    public void delete() {
        JpaOperations.delete(this);
    }
    
    public boolean isPersistent() {
        return JpaOperations.isPersistent(this);
    }

    // Queries
    
    public static <T extends EntityBase> T findById(Object id) {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends EntityBase> Query<T> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends EntityBase> Query<T> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public static long count() {
        throw new RuntimeException("Should never be called");
    }

    public static long count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static long deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public static long delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }
}
