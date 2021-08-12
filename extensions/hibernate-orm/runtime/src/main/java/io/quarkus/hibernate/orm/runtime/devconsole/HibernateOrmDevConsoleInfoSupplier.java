package io.quarkus.hibernate.orm.runtime.devconsole;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        INSTANCE.persistenceUnits.add(persistenceUnitName);

        String createSchema = generateDDL(SchemaExport.Action.CREATE, metadata, serviceRegistry, importFile);
        INSTANCE.createDDLs.put(persistenceUnitName, createSchema);

        String dropSchema = generateDDL(SchemaExport.Action.DROP, metadata, serviceRegistry, importFile);
        INSTANCE.dropDDLs.put(persistenceUnitName, dropSchema);

        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            List<EntityInfo> list = INSTANCE.managedEntities.computeIfAbsent(persistenceUnitName,
                    pu -> new ArrayList<>());
            list.add(new HibernateOrmDevConsoleInfoSupplier.EntityInfo(entityBinding.getClassName(),
                    entityBinding.getTable().getName(), persistenceUnitName));
        }

        for (NamedQueryDefinition queryDefinition : metadata.getNamedQueryDefinitions()) {
            List<QueryInfo> list = INSTANCE.namedQueries.computeIfAbsent(persistenceUnitName,
                    pu -> new ArrayList<>());
            list.add(new QueryInfo(queryDefinition));
        }

        for (NamedSQLQueryDefinition staticQueryDefinition : metadata.getNamedNativeQueryDefinitions()) {
            List<QueryInfo> list = INSTANCE.namedNativeQueries.computeIfAbsent(persistenceUnitName,
                    pu -> new ArrayList<>());
            list.add(new QueryInfo(staticQueryDefinition));
        }
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

        private final List<String> persistenceUnits = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, List<EntityInfo>> managedEntities = new ConcurrentHashMap<>();
        private final Map<String, List<QueryInfo>> namedQueries = new ConcurrentHashMap<>();
        private final Map<String, List<QueryInfo>> namedNativeQueries = new ConcurrentHashMap<>();
        private final Map<String, String> createDDLs = new ConcurrentHashMap<>();
        private final Map<String, String> dropDDLs = new ConcurrentHashMap<>();
        private boolean sorted;

        public List<String> getPersistenceUnits() {
            if (!sorted) {
                sorted = true;
                persistenceUnits.sort(new PersistenceUnitNameComparator());
            }
            return persistenceUnits;
        }

        public Map<String, String> getCreateDDLs() {
            return createDDLs;
        }

        public Map<String, String> getDropDDLs() {
            return dropDDLs;
        }

        public List<EntityInfo> getManagedEntities(String pu) {
            return managedEntities.getOrDefault(pu, Collections.emptyList());
        }

        public List<QueryInfo> getNamedQueries(String pu) {
            return namedQueries.getOrDefault(pu, Collections.emptyList());
        }

        public List<QueryInfo> getNamedNativeQueries(String pu) {
            return namedNativeQueries.getOrDefault(pu, Collections.emptyList());
        }

        public List<QueryInfo> getAllNamedQueries(String pu) {
            ArrayList<QueryInfo> allQueries = new ArrayList<>();
            allQueries.addAll(namedQueries.getOrDefault(pu, Collections.emptyList()));
            allQueries.addAll(namedNativeQueries.getOrDefault(pu, Collections.emptyList()));
            return allQueries;
        }

        public int getNumberOfNamedQueries() {
            return namedQueries.values().stream().mapToInt(List::size).reduce(Integer::sum).orElse(0)
                    + namedNativeQueries.values().stream().mapToInt(List::size).reduce(Integer::sum).orElse(0);
        }

        public int getNumberOfEntities() {
            return managedEntities.values().stream().mapToInt(List::size).reduce(Integer::sum).orElse(0);
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
