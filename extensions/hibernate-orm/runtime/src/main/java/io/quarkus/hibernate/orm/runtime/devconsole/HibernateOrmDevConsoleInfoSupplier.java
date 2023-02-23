package io.quarkus.hibernate.orm.runtime.devconsole;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedQueryDefinition;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerCollectingImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class HibernateOrmDevConsoleInfoSupplier implements Supplier<HibernateOrmDevConsoleInfoSupplier.PersistenceUnitsInfo> {

    public static final PersistenceUnitsInfo INSTANCE = new PersistenceUnitsInfo();

    public static void pushPersistenceUnit(String persistenceUnitName,
            Metadata metadata, ServiceRegistry serviceRegistry, String importFile) {
        List<EntityInfo> managedEntities = new ArrayList<>();
        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            managedEntities.add(new EntityInfo(entityBinding.getClassName(), entityBinding.getTable().getName()));
        }

        List<QueryInfo> namedQueries = new ArrayList<>();
        {
            List<NamedHqlQueryDefinition> namedQueriesHqlDefs = new ArrayList<>();
            metadata.visitNamedHqlQueryDefinitions(namedQueriesHqlDefs::add);
            for (NamedHqlQueryDefinition queryDefinition : namedQueriesHqlDefs) {
                namedQueries.add(new QueryInfo(queryDefinition));
            }
        }

        List<QueryInfo> namedNativeQueries = new ArrayList<>();
        {
            List<NamedNativeQueryDefinition> namedNativeQueriesNativeDefs = new ArrayList<>();
            metadata.visitNamedNativeQueryDefinitions(namedNativeQueriesNativeDefs::add);
            for (NamedNativeQueryDefinition staticQueryDefinition : namedNativeQueriesNativeDefs) {
                namedNativeQueries.add(new QueryInfo(staticQueryDefinition));
            }
        }

        String createDDL = generateDDL(Action.CREATE, metadata, serviceRegistry, importFile);
        String dropDDL = generateDDL(Action.DROP, metadata, serviceRegistry, importFile);

        INSTANCE.persistenceUnits.put(persistenceUnitName, new PersistenceUnitInfo(persistenceUnitName, managedEntities,
                namedQueries, namedNativeQueries, createDDL, dropDDL));
    }

    public static void clearData() {
        INSTANCE.persistenceUnits.clear();
    }

    private static String generateDDL(Action action, Metadata metadata, ServiceRegistry ssr,
            String importFiles) {
        //TODO see https://hibernate.atlassian.net/browse/HHH-16207
        final HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool) ssr.getService(SchemaManagementTool.class);
        Map<String, Object> config = new HashMap<>(ssr.getService(ConfigurationService.class).getSettings());
        config.put(AvailableSettings.HBM2DDL_DELIMITER, ";");
        config.put(AvailableSettings.FORMAT_SQL, true);
        config.put(AvailableSettings.HBM2DDL_IMPORT_FILES, importFiles);
        ExceptionHandlerCollectingImpl exceptionHandler = new ExceptionHandlerCollectingImpl();
        try {
            final ExecutionOptions executionOptions = SchemaManagementToolCoordinator.buildExecutionOptions(
                    config,
                    exceptionHandler);
            StringWriter writer = new StringWriter();
            final SourceDescriptor source = new SourceDescriptor() {
                @Override
                public SourceType getSourceType() {
                    return SourceType.METADATA;
                }

                @Override
                public ScriptSourceInput getScriptSourceInput() {
                    return null;
                }
            };
            final TargetDescriptor target = new TargetDescriptor() {
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
            };
            if (action == Action.DROP) {
                SchemaDropper schemaDropper = tool.getSchemaDropper(executionOptions.getConfigurationValues());
                schemaDropper.doDrop(metadata, executionOptions, ContributableMatcher.ALL, source, target);
            } else if (action == Action.CREATE) {
                SchemaCreator schemaDropper = tool.getSchemaCreator(executionOptions.getConfigurationValues());
                schemaDropper.doCreation(metadata, executionOptions, ContributableMatcher.ALL, source, target);
            }
            return writer.toString();
        } catch (RuntimeException e) {
            //TODO unroll the exceptionHandler ?
            StringWriter stackTraceWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTraceWriter));
            return "Could not generate DDL: \n" + stackTraceWriter.toString();
        }
    }

    @Override
    public PersistenceUnitsInfo get() {
        return INSTANCE;
    }

    public static class PersistenceUnitsInfo {

        private final Map<String, PersistenceUnitInfo> persistenceUnits = Collections
                .synchronizedMap(new TreeMap<>(new PersistenceUnitUtil.PersistenceUnitNameComparator()));

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

        public QueryInfo(NamedHqlQueryDefinition queryDefinition) {
            this.name = queryDefinition.getRegistrationName();
            this.query = queryDefinition.getHqlString();
            this.cacheable = extractIsCacheable(queryDefinition);
            this.lockMode = extractLockOptions(queryDefinition);
            this.type = "JPQL";
        }

        public QueryInfo(NamedNativeQueryDefinition nativeQueryDefinition) {
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
