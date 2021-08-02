package io.quarkus.hibernate.orm.runtime.devconsole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.persistence.spi.PersistenceProviderResolverHolder;

import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitsHolder;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.mapping.PersistentClass;

import io.quarkus.hibernate.orm.runtime.recording.RecordedState;

public class PersistenceUnitInfoSupplier implements Supplier<PersistenceUnitInfoSupplier.PersistenceUnitsInfo> {

    @Override
    public PersistenceUnitsInfo get() {
        return composePersistenceUnitsInfo(PersistenceUnitsHolder.getPersistenceUnitDescriptors());
    }

    private PersistenceUnitsInfo composePersistenceUnitsInfo(List<PersistenceUnitDescriptor> persistenceUnits) {
        PersistenceUnitsInfo persistenceUnitsInfo = new PersistenceUnitsInfo();

        FastBootHibernatePersistenceProvider persistenceProvider = (FastBootHibernatePersistenceProvider) PersistenceProviderResolverHolder
                .getPersistenceProviderResolver().getPersistenceProviders().get(0);

        for (PersistenceUnitDescriptor descriptor : persistenceUnits) {
            persistenceUnitsInfo.getPersistenceUnits().add(descriptor);
            RecordedState recordedState = PersistenceUnitsHolder.popRecordedState(descriptor.getName());

            String schema = persistenceProvider.generateSchemaToString(descriptor.getName(), null);
            persistenceUnitsInfo.createDDLs.put(descriptor.getName(), schema);

            for (String className : descriptor.getManagedClassNames()) {
                PersistentClass entityBinding = recordedState.getMetadata().getEntityBinding(className);
                persistenceUnitsInfo.managedEntities.add(new PersistenceUnitInfoSupplier.EntityInfo(className,
                        entityBinding.getTable().getName(), descriptor.getName()));
            }

            for (NamedQueryDefinition queryDefinition : recordedState.getMetadata().getNamedQueryDefinitions()) {
                persistenceUnitsInfo.namedQueries.add(new QueryInfo(queryDefinition));
            }

            for (NamedSQLQueryDefinition staticQueryDefinition : recordedState.getMetadata().getNamedNativeQueryDefinitions()) {
                persistenceUnitsInfo.namedNativeQueries.add(new QueryInfo(staticQueryDefinition));
            }
        }

        return persistenceUnitsInfo;
    }

    public static class PersistenceUnitsInfo {

        private final List<PersistenceUnitDescriptor> persistenceUnits = new ArrayList<>();
        private final List<EntityInfo> managedEntities = new ArrayList<>();
        private final List<QueryInfo> namedQueries = new ArrayList<>();
        private final List<QueryInfo> namedNativeQueries = new ArrayList<>();
        private final Map<String, String> createDDLs = new HashMap<>();

        public List<PersistenceUnitDescriptor> getPersistenceUnits() {
            return persistenceUnits;
        }

        public Map<String, String> getCreateDDLs() {
            return createDDLs;
        }

        public List<EntityInfo> getManagedEntities() {
            return managedEntities;
        }

        public List<QueryInfo> getNamedQueries() {
            return namedQueries;
        }

        public List<QueryInfo> getNamedNativeQueries() {
            return namedNativeQueries;
        }

        public List<QueryInfo> getAllNamedQueries() {
            ArrayList<QueryInfo> allQueries = new ArrayList<>();
            allQueries.addAll(namedQueries);
            allQueries.addAll(namedNativeQueries);
            return allQueries;
        }

    }

    public static class EntityInfo {

        private final String className;
        private final String tableName;
        private final String persistenceUnitName;

        public EntityInfo(String className, String tableName, String persistenceUnitName) {
            this.className = className;
            this.tableName = tableName;
            this.persistenceUnitName = persistenceUnitName;
        }

        public String getClassName() {
            return className;
        }

        public String getTableName() {
            return tableName;
        }

        public String getPersistenceUnitName() {
            return persistenceUnitName;
        }
    }

    public static class QueryInfo {

        private final String name;
        private final String query;
        private final boolean cacheable;
        private final String lockMode;
        private final String type;

        public QueryInfo(NamedQueryDefinition queryDefinition) {
            this.name = queryDefinition.getName();
            this.query = queryDefinition.getQuery();
            this.cacheable = queryDefinition.isCacheable();
            LockOptions lockOptions = queryDefinition.getLockOptions();
            this.lockMode = lockOptions != null && lockOptions.getLockMode() != null
                    ? lockOptions.getLockMode().name()
                    : "";
            this.type = queryDefinition instanceof NamedSQLQueryDefinition ? "native" : "JPQL";
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

    }
}
