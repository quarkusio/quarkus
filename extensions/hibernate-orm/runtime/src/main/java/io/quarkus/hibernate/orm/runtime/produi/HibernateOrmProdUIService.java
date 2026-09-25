package io.quarkus.hibernate.orm.runtime.produi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.named.NamedObjectRepository;

import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.mutiny.tuples.Tuple2;

/**
 * Read-only Prod UI view of the Hibernate ORM persistence units. For each unit
 * it exposes the managed entities and the registered named queries derived from
 * the running {@link SessionFactoryImplementor}. It deliberately omits the Dev
 * UI's HQL console (arbitrary query execution) and the create / drop / update
 * DDL scripts, and exposes no datasource URLs or credentials.
 */
@ApplicationScoped
public class HibernateOrmProdUIService {

    @Inject
    JPAConfig jpaConfig;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Hibernate ORM persistence units, their managed entities and named queries")
    public List<PersistenceUnitInfo> getPersistenceUnits() {
        List<PersistenceUnitInfo> result = new ArrayList<>();
        for (Tuple2<String, EntityManagerFactory> entry : jpaConfig.getEntityManagerFactories()) {
            String name = entry.getItem1();
            try {
                SessionFactoryImplementor sessionFactory = entry.getItem2().unwrap(SessionFactoryImplementor.class);
                result.add(new PersistenceUnitInfo(name, collectEntities(sessionFactory),
                        collectNamedQueries(sessionFactory), null));
            } catch (RuntimeException e) {
                result.add(new PersistenceUnitInfo(name, List.of(), List.of(), e.getMessage()));
            }
        }
        result.sort(Comparator.comparing(PersistenceUnitInfo::name));
        return result;
    }

    private static List<EntityInfo> collectEntities(SessionFactoryImplementor sessionFactory) {
        List<EntityInfo> entities = new ArrayList<>();
        sessionFactory.getMappingMetamodel().forEachEntityDescriptor(persister -> {
            entities.add(new EntityInfo(persister.getJpaEntityName(), entityClassName(persister),
                    safeTableName(persister)));
        });
        entities.sort(Comparator.comparing(EntityInfo::name));
        return entities;
    }

    private static String entityClassName(EntityPersister persister) {
        Class<?> mappedClass = persister.getMappedClass();
        return mappedClass != null ? mappedClass.getName() : persister.getEntityName();
    }

    private static String safeTableName(EntityPersister persister) {
        try {
            return persister.getTableName();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static List<QueryInfo> collectNamedQueries(SessionFactoryImplementor sessionFactory) {
        List<QueryInfo> queries = new ArrayList<>();
        NamedObjectRepository repository = sessionFactory.getQueryEngine().getNamedObjectRepository();
        repository.visitSqmQueryMementos(memento -> queries.add(new QueryInfo(memento.getRegistrationName(),
                memento.getHqlString(), "JPQL", Boolean.TRUE.equals(memento.getCacheable()),
                lockMode(memento.getLockOptions()))));
        repository.visitNativeQueryMementos(memento -> queries.add(new QueryInfo(memento.getRegistrationName(),
                memento.getSqlString(), "native", Boolean.TRUE.equals(memento.getCacheable()), "")));
        queries.sort(Comparator.comparing(QueryInfo::name));
        return queries;
    }

    private static String lockMode(LockOptions lockOptions) {
        if (lockOptions == null) {
            return "";
        }
        LockMode lockMode = lockOptions.getLockMode();
        return lockMode == null || lockMode == LockMode.NONE ? "" : lockMode.name();
    }

    public record PersistenceUnitInfo(String name, List<EntityInfo> entities, List<QueryInfo> namedQueries,
            String error) {
    }

    public record EntityInfo(String name, String className, String tableName) {
    }

    public record QueryInfo(String name, String query, String type, boolean cacheable, String lockMode) {
    }
}
