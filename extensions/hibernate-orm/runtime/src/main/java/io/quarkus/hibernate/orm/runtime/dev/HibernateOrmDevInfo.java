package io.quarkus.hibernate.orm.runtime.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedQueryDefinition;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class HibernateOrmDevInfo {

    private final Map<String, PersistenceUnit> persistenceUnits = Collections
            .synchronizedMap(new TreeMap<>(new PersistenceUnitUtil.PersistenceUnitNameComparator()));

    public Collection<PersistenceUnit> getPersistenceUnits() {
        return persistenceUnits.values();
    }

    void add(PersistenceUnit persistenceUnitInfo) {
        persistenceUnits.put(persistenceUnitInfo.getName(), persistenceUnitInfo);
    }

    public int getNumberOfNamedQueries() {
        return persistenceUnits.values().stream().map(PersistenceUnit::getAllNamedQueries)
                .mapToInt(List::size).reduce(Integer::sum).orElse(0);
    }

    public int getNumberOfEntities() {
        return persistenceUnits.values().stream().map(PersistenceUnit::getManagedEntities)
                .mapToInt(List::size).reduce(Integer::sum).orElse(0);
    }

    public static class PersistenceUnit {

        private final String name;
        private final List<Entity> managedEntities;
        private final List<Query> namedQueries;
        private final List<Query> namedNativeQueries;
        private String createDDL;
        private String dropDDL;
        private String updateDDL;
        private final Supplier<String> createDDLSupplier;
        private final Supplier<String> dropDDLSupplier;
        private final Supplier<String> updateDDLSupplier;

        public PersistenceUnit(String name, List<Entity> managedEntities,
                List<Query> namedQueries,
                List<Query> namedNativeQueries, Supplier<String> createDDL, Supplier<String> dropDDL,
                Supplier<String> updateDDLSupplier) {
            this.name = name;
            this.managedEntities = managedEntities;
            this.namedQueries = namedQueries;
            this.namedNativeQueries = namedNativeQueries;
            this.createDDLSupplier = createDDL;
            this.dropDDLSupplier = dropDDL;
            this.updateDDLSupplier = updateDDLSupplier;
        }

        public String getName() {
            return name;
        }

        public List<Entity> getManagedEntities() {
            return managedEntities;
        }

        public List<Query> getNamedQueries() {
            return namedQueries;
        }

        public List<Query> getNamedNativeQueries() {
            return namedNativeQueries;
        }

        public List<Query> getAllNamedQueries() {
            ArrayList<Query> allQueries = new ArrayList<>();
            allQueries.addAll(namedQueries);
            allQueries.addAll(namedNativeQueries);
            return allQueries;
        }

        public synchronized String getCreateDDL() {
            if (createDDL == null) {
                createDDL = createDDLSupplier.get();
            }
            return createDDL;
        }

        public synchronized String getDropDDL() {
            if (dropDDL == null) {
                dropDDL = dropDDLSupplier.get();
            }
            return dropDDL;
        }

        public synchronized String getUpdateDDL() {
            if (updateDDL == null) {
                updateDDL = updateDDLSupplier.get();
            }
            return updateDDL;
        }

    }

    public static class Entity {

        private final String className;
        private final String tableName;

        public Entity(String className, String tableName) {
            this.className = className;
            this.tableName = tableName;
        }

        public String getClassName() {
            return className;
        }

        public String getTableName() {
            return tableName;
        }

    }

    public static class Query {

        private final String name;
        private final String query;
        private final boolean cacheable;
        private final String lockMode;
        private final String type;

        public Query(NamedHqlQueryDefinition queryDefinition) {
            this.name = queryDefinition.getRegistrationName();
            this.query = queryDefinition.getHqlString();
            this.cacheable = extractIsCacheable(queryDefinition);
            this.lockMode = extractLockOptions(queryDefinition);
            this.type = "JPQL";
        }

        public Query(NamedNativeQueryDefinition nativeQueryDefinition) {
            this.name = nativeQueryDefinition.getRegistrationName();
            this.query = nativeQueryDefinition.getSqlQueryString();
            this.cacheable = extractIsCacheable(nativeQueryDefinition);
            this.lockMode = extractLockOptions(nativeQueryDefinition);
            this.type = "native";
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query;
        }

        public boolean isCacheable() {
            return cacheable;
        }

        public String getLockMode() {
            return lockMode;
        }

        public String getType() {
            return type;
        }

        private static boolean extractIsCacheable(NamedQueryDefinition definition) {
            //TODO cleanup and expose this properly in an SPI/API?
            if (definition instanceof AbstractNamedQueryDefinition) {
                AbstractNamedQueryDefinition def = (AbstractNamedQueryDefinition) definition;
                if (def.getCacheable() == Boolean.TRUE) {
                    return true;
                }
            }
            return false;
        }

        private static String extractLockOptions(NamedQueryDefinition definition) {
            //TODO cleanup and expose this properly in an SPI/API?
            if (definition instanceof AbstractNamedQueryDefinition) {
                final AbstractNamedQueryDefinition def = (AbstractNamedQueryDefinition) definition;
                final LockOptions lockOptions = def.getLockOptions();
                if (lockOptions != null && lockOptions.getLockMode() != null) {
                    return lockOptions.getLockMode().name();
                }
            }
            return "";
        }
    }
}
