package io.quarkus.hibernate.orm.panache.runtime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

//TODO this class is only needed by the Spring Data JPA module and would be placed there it it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class AdditionalJpaOperations {

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, String query, String countQuery, Sort sort,
            Map<String, Object> params) {
        String findQuery = JpaOperations.createFindQuery(entityClass, query, JpaOperations.paramCount(params));
        EntityManager em = JpaOperations.getEntityManager();
        Query jpaQuery = em.createQuery(sort != null ? findQuery + JpaOperations.toOrderBy(sort) : findQuery);
        JpaOperations.bindParameters(jpaQuery, params);
        return new CustomCountPanacheQuery(em, jpaQuery, findQuery, countQuery, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, String countQuery, Sort sort,
            Parameters parameters) {
        return find(entityClass, query, countQuery, sort, parameters.map());
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, String query, String countQuery, Sort sort, Object... params) {
        String findQuery = JpaOperations.createFindQuery(entityClass, query, JpaOperations.paramCount(params));
        EntityManager em = JpaOperations.getEntityManager();
        Query jpaQuery = em.createQuery(sort != null ? findQuery + JpaOperations.toOrderBy(sort) : findQuery);
        JpaOperations.bindParameters(jpaQuery, params);
        return new CustomCountPanacheQuery(em, jpaQuery, findQuery, countQuery, params);
    }

    public static long deleteAllWithCascade(Class<?> entityClass) {
        EntityManager em = JpaOperations.getEntityManager();
        //detecting the case where there are cascade-delete associations, and do the the bulk delete query otherwise.
        if (deleteOnCascadeDetected(entityClass)) {
            int count = 0;
            List<?> objects = JpaOperations.listAll(entityClass);
            for (Object entity : objects) {
                em.remove(entity);
                count++;
            }
            return count;
        }
        return JpaOperations.deleteAll(entityClass);
    }

    /**
     * Detects if cascading delete is needed. The delete-cascading is needed when associations with cascade delete enabled
     * {@link javax.persistence.OneToMany#cascade()} and also on entities containing a collection of elements
     * {@link javax.persistence.ElementCollection}
     *
     * @param entityClass
     * @return true if cascading delete is needed. False otherwise
     */
    private static boolean deleteOnCascadeDetected(Class<?> entityClass) {
        EntityManager em = JpaOperations.getEntityManager();
        Metamodel metamodel = em.getMetamodel();
        EntityType<?> entity1 = metamodel.entity(entityClass);
        Set<Attribute<?, ?>> declaredAttributes = ((EntityTypeImpl) entity1).getDeclaredAttributes();

        CascadeStyle[] propertyCascadeStyles = em.unwrap(SessionImplementor.class)
                .getEntityPersister(entityClass.getName(), null)
                .getPropertyCascadeStyles();
        boolean doCascade = Arrays.stream(propertyCascadeStyles)
                .anyMatch(cascadeStyle -> cascadeStyle.doCascade(CascadingActions.DELETE));
        boolean hasElementCollection = declaredAttributes.stream().filter(attribute -> attribute.getPersistentAttributeType()
                .equals(Attribute.PersistentAttributeType.ELEMENT_COLLECTION)).count() > 0;
        return doCascade || hasElementCollection;

    }

    public static long deleteWithCascade(Class<?> entityClass, String query, Object... params) {
        EntityManager em = JpaOperations.getEntityManager();
        if (deleteOnCascadeDetected(entityClass)) {
            int count = 0;
            List<?> objects = JpaOperations.find(entityClass, query, params).list();
            for (Object entity : objects) {
                em.remove(entity);
                count++;
            }
            return count;
        }
        return JpaOperations.delete(entityClass, query, params);
    }

    public static long deleteWithCascade(Class<?> entityClass, String query, Map<String, Object> params) {
        EntityManager em = JpaOperations.getEntityManager();
        if (deleteOnCascadeDetected(entityClass)) {
            int count = 0;
            List<?> objects = JpaOperations.find(entityClass, query, params).list();
            for (Object entity : objects) {
                em.remove(entity);
                count++;
            }
            return count;
        }
        return JpaOperations.delete(entityClass, query, params);
    }

    public static long deleteWithCascade(Class<?> entityClass, String query, Parameters params) {
        return deleteWithCascade(entityClass, query, params.map());
    }
}
