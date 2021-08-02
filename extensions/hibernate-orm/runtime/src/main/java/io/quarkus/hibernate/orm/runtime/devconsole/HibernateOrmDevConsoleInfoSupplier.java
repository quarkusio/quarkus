package io.quarkus.hibernate.orm.runtime.devconsole;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

public class HibernateOrmDevConsoleInfoSupplier implements Supplier<HibernateOrmDevConsoleInfoSupplier.PersistenceUnitsInfo> {

    private static final PersistenceUnitsInfo INSTANCE = new PersistenceUnitsInfo();

    @Override
    public PersistenceUnitsInfo get() {
        return INSTANCE;
    }

    public static void pushPersistenceUnit(String persistenceUnitName,
            Metadata metadata, ServiceRegistry serviceRegistry) {
        INSTANCE.getPersistenceUnits().add(persistenceUnitName);

        String schema = generateDDL(SchemaExport.Action.CREATE, metadata, serviceRegistry);
        INSTANCE.createDDLs.put(persistenceUnitName, schema);

        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            INSTANCE.managedEntities.add(new HibernateOrmDevConsoleInfoSupplier.EntityInfo(entityBinding.getClassName(),
                    entityBinding.getTable().getName(), persistenceUnitName));
        }

        for (NamedQueryDefinition queryDefinition : metadata.getNamedQueryDefinitions()) {
            INSTANCE.namedQueries.add(new QueryInfo(queryDefinition));
        }

        for (NamedSQLQueryDefinition staticQueryDefinition : metadata.getNamedNativeQueryDefinitions()) {
            INSTANCE.namedNativeQueries.add(new QueryInfo(staticQueryDefinition));
        }
    }

    private static String generateDDL(SchemaExport.Action action, Metadata metadata, ServiceRegistry serviceRegistry) {
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.setFormat(true);
        schemaExport.setDelimiter(";");
        StringWriter writer = new StringWriter();
        schemaExport.doExecution(action, false, metadata, serviceRegistry,
                new TargetDescriptor() {
                    @Override
                    public EnumSet<TargetType> getTargetTypes() {
                        return EnumSet.of(TargetType.SCRIPT);
                    }

                    @Override
                    public ScriptTargetOutput getScriptTargetOutput() {
                        return new ScriptTargetOutputToWriter(writer) {
                            @Override
                            public void accept(String command) {
                                super.accept(command);
                            }
                        };
                    }
                });
        return writer.toString();
    }

    public static class PersistenceUnitsInfo {

        private final List<String> persistenceUnits = new ArrayList<>();
        private final List<EntityInfo> managedEntities = new ArrayList<>();
        private final List<QueryInfo> namedQueries = new ArrayList<>();
        private final List<QueryInfo> namedNativeQueries = new ArrayList<>();
        private final Map<String, String> createDDLs = new HashMap<>();

        public List<String> getPersistenceUnits() {
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
