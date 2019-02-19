package org.jboss.panache.jpa;

public interface DaoBase<Entity> {

    // Operations
    
    public default void save(Entity entity) {
        JpaOperations.save(entity);
    }
    
    public default void delete(Entity entity) {
        JpaOperations.delete(entity);
    }
    
    public default boolean isPersistent(Entity entity) {
        return JpaOperations.isPersistent(entity);
    }

    // Queries
    
    public default Entity findById(Object id) {
        throw new RuntimeException("Should never be called");
    }

    public default Query<Entity> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default Query<Entity> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public default long count() {
        throw new RuntimeException("Should never be called");
    }

    public default long count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default long deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public default long delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }
}
