package io.quarkus.hibernate.orm.runtime.devconsole;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    private static final String DEFAULT = "<default>";

    private static final PersistenceUnitsInfo INSTANCE = new PersistenceUnitsInfo();

    public static void pushPersistenceUnit(String persistenceUnitName,
            Metadata metadata, ServiceRegistry serviceRegistry, String importFile) {
        List<EntityInfo> managedEntities = new ArrayList<>();
        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            managedEntities.add(new EntityInfo(entityBinding.getClassName(), entityBinding.getTable().getName()));
        }

        List<QueryInfo> namedQueries = new ArrayList<>();
        for (NamedQueryDefinition queryDefinition : metadata.getNamedQueryDefinitions()) {
            namedQueries.add(new QueryInfo(queryDefinition));
        }

        List<QueryInfo> namedNativeQueries = new ArrayList<>();
        for (NamedSQLQueryDefinition staticQueryDefinition : metadata.getNamedNativeQueryDefinitions()) {
            namedNativeQueries.add(new QueryInfo(staticQueryDefinition));
        }

        String createDDL = generateDDL(SchemaExport.Action.CREATE, metadata, serviceRegistry, importFile);
        String dropDDL = generateDDL(SchemaExport.Action.DROP, metadata, serviceRegistry, importFile);

        INSTANCE.persistenceUnits.put(persistenceUnitName, new PersistenceUnitInfo(persistenceUnitName, managedEntities,
                namedQueries, namedNativeQueries, createDDL, dropDDL));
    }

    public static void clearData() {
        INSTANCE.persistenceUnits.clear();
    }

    private static String generateDDL(SchemaExport.Action action, Metadata metadata, ServiceRegistry serviceRegistry,
            String importFiles) {
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.setFormat(true);
        schemaExport.setDelimiter(";");
        schemaExport.setImportFiles(importFiles);
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

    @Override
    public PersistenceUnitsInfo get() {
        return INSTANCE;
    }

    public static class PersistenceUnitsInfo {

        private final Map<String, PersistenceUnitInfo> persistenceUnits = Collections
                .synchronizedMap(new TreeMap<>(new PersistenceUnitNameComparator()));

        public Collection<PersistenceUnitInfo> getPersistenceUnits() {
            return persistenceUnits.values();
        }

        public int getNumberOfNamedQueries() {
            return persistenceUnits.values().stream().map(PersistenceUnitInfo::getAllNamedQueries)
                    .mapToInt(List::size).reduce(Integer::sum).orElse(0);
        }

        public int getNumberOfEntities() {
            return persistenceUnits.values().stream().map(PersistenceUnitInfo::getManagedEntities)
                    .mapToInt(List::size).reduce(Integer::sum).orElse(0);
        }

    }

    public static class PersistenceUnitInfo {

        private final String name;
        private final List<EntityInfo> managedEntities;
        private final List<QueryInfo> namedQueries;
        private final List<QueryInfo> namedNativeQueries;
        private final String createDDL;
        private final String dropDDL;

        public PersistenceUnitInfo(String name, List<EntityInfo> managedEntities, List<QueryInfo> namedQueries,
                List<QueryInfo> namedNativeQueries, String createDDL, String dropDDL) {
            this.name = name;
            this.managedEntities = managedEntities;
            this.namedQueries = namedQueries;
            this.namedNativeQueries = namedNativeQueries;
            this.createDDL = createDDL;
            this.dropDDL = dropDDL;
        }

        public String getName() {
            return name;
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

        public String getCreateDDL() {
            return createDDL;
        }

        public String getDropDDL() {
            return dropDDL;
        }

    }

    public static class EntityInfo {

        private final String className;
        private final String tableName;

        public EntityInfo(String className, String tableName) {
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

    static class PersistenceUnitNameComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (DEFAULT.equals(o1)) {
                return -1;
            } else if (DEFAULT.equals(o2)) {
                return +1;
            } else {
                return o1.compareTo(o2);
            }
        }
    }
}
