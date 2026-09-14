package io.quarkus.hibernate.orm.runtime.schema;

import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;

import java.io.StringWriter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DatabaseSchemaProvider;
import io.quarkus.runtime.LaunchMode;

public class SchemaManagementIntegrator implements Integrator, DatabaseSchemaProvider {

    private static final Logger log = Logger.getLogger(SchemaManagementIntegrator.class);

    private static final Map<String, Holder> metadataMap = new ConcurrentHashMap<>();
    private static final Map<String, String> datasourceToPuMap = new ConcurrentHashMap<>();
    private static final Map<SessionFactoryImplementor, String> nameCache = Collections
            .synchronizedMap(new IdentityHashMap<>());

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        String name = defaultName(sessionFactory);
        metadataMap.put(name, new Holder(metadata, sessionFactory, sessionFactory.getServiceRegistry()));
        nameCache.put(sessionFactory, name);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        final String name = nameCache.remove(sessionFactory);
        if (name != null) {
            metadataMap.remove(name);
        }
    }

    public static void clearDsMap() {
        datasourceToPuMap.clear();
    }

    public static void mapDatasource(String datasource, String pu) {
        datasourceToPuMap.put(datasource, pu);
    }

    static String defaultName(SessionFactoryImplementor sf) {
        String name = sf.getName();
        if (name != null) {
            return name;
        }
        Object persistenceUnitName = sf.getProperties().get(PERSISTENCE_UNIT_NAME);
        if (persistenceUnitName != null) {
            return persistenceUnitName.toString();
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }

    public static void recreateDatabases() {
        if (!LaunchMode.current().isDevOrTest()) {
            throw new IllegalStateException("Can only be used in dev or test mode");
        }
        for (String val : metadataMap.keySet()) {
            recreateDatabase(val);
        }
    }

    public static void recreateDatabase(String name) {
        if (!LaunchMode.current().isDevOrTest()) {
            throw new IllegalStateException("Can only be used in dev or test mode");
        }
        Holder holder = metadataMap.get(name);

        ServiceRegistry serviceRegistry = holder.sessionFactory.getServiceRegistry();
        SimpleExecutionOptions executionOptions = new SimpleExecutionOptions(serviceRegistry);
        Action schemaGenerationDatabaseAction = databaseAction(executionOptions);
        if (!Action.NONE.equals(schemaGenerationDatabaseAction) && !Action.POPULATE.equals(schemaGenerationDatabaseAction)) {
            //if this is none (or populate, which doesn't touch the schema) we assume another framework is doing this (e.g. flyway)
            SchemaManagementTool schemaManagementTool = serviceRegistry
                    .getService(SchemaManagementTool.class);
            // Drop and create the schema exactly like on startup,
            // so that the schema init script and the data init script get executed too
            SchemaManagementToolCoordinator.performDatabaseAction(Action.CREATE, holder.metadata, schemaManagementTool,
                    serviceRegistry, executionOptions, ContributableMatcher.ALL);
        }
        //we still clear caches though
        evictCaches(holder);
    }

    public static void populatePersistenceUnits() {
        if (!LaunchMode.current().isDevOrTest()) {
            throw new IllegalStateException("Can only be used in dev or test mode");
        }
        for (String val : metadataMap.keySet()) {
            populatePersistenceUnit(val);
        }
    }

    /**
     * Executes the data init script again, for persistence units whose schema is managed by another framework
     * (e.g. Flyway) and thus was not recreated by {@link #recreateDatabase(String)}.
     * Must be called after that other framework is done resetting the schema.
     */
    public static void populatePersistenceUnit(String name) {
        if (!LaunchMode.current().isDevOrTest()) {
            throw new IllegalStateException("Can only be used in dev or test mode");
        }
        Holder holder = metadataMap.get(name);

        ServiceRegistry serviceRegistry = holder.sessionFactory.getServiceRegistry();
        SimpleExecutionOptions executionOptions = new SimpleExecutionOptions(serviceRegistry);
        if (Action.POPULATE.equals(databaseAction(executionOptions))) {
            SchemaManagementTool schemaManagementTool = serviceRegistry
                    .getService(SchemaManagementTool.class);
            SchemaManagementToolCoordinator.performDatabaseAction(Action.POPULATE, holder.metadata, schemaManagementTool,
                    serviceRegistry, executionOptions, ContributableMatcher.ALL);
            evictCaches(holder);
        }
    }

    private static Action databaseAction(SimpleExecutionOptions executionOptions) {
        return Action.interpretJpaSetting(executionOptions.getConfigurationValues()
                .get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION));
    }

    private static void evictCaches(Holder holder) {
        holder.sessionFactory.getCache().evictAll();
        holder.sessionFactory.getCache().evictQueries();
    }

    public static void runPostBootValidation(String name) {
        if (!LaunchMode.current().isDevOrTest()) {
            throw new IllegalStateException("Can only be used in dev or test mode");
        }
        try {

            Holder val = metadataMap.get(name);
            if (val == null) {
                return;
            }

            //if this is none we assume another framework is doing this (e.g. flyway)
            ServiceRegistry serviceRegistry = val.sessionFactory.getServiceRegistry();
            SchemaManagementTool schemaManagementTool = serviceRegistry
                    .getService(SchemaManagementTool.class);
            SimpleExecutionOptions executionOptions = new SimpleExecutionOptions(serviceRegistry);
            SchemaValidator validator = schemaManagementTool.getSchemaValidator(executionOptions.getConfigurationValues());
            try {
                validator.doValidation(val.metadata, executionOptions, ContributableMatcher.ALL);
            } catch (SchemaManagementException e) {
                log.error("Failed to validate Schema: " + e.getMessage());
                SchemaMigrator migrator = schemaManagementTool.getSchemaMigrator(executionOptions.getConfigurationValues());

                StringWriter writer = new StringWriter();
                migrator.doMigration(val.metadata, executionOptions, ContributableMatcher.ALL, new TargetDescriptor() {
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
                log.error(
                        "The following SQL may resolve the database issues, as generated by the Hibernate schema migration tool. WARNING: You must manually verify this SQL is correct, this is a best effort guess, do not copy/paste it without verifying that it does what you expect.\n\n"
                                + writer.toString());
            }
        } catch (Throwable t) {
            log.error("Failed to run post-boot validation", t);
        }
    }

    @Override
    public void resetDatabase(String dbName) {
        String name = datasourceToPuMap.get(dbName);
        if (name == null) {
            //not an hibernate DS
            return;
        }
        recreateDatabase(name);
    }

    @Override
    public void resetAllDatabases() {
        recreateDatabases();
    }

    @Override
    public void populateDatabase(String dbName) {
        String name = datasourceToPuMap.get(dbName);
        if (name == null) {
            //not an hibernate DS
            return;
        }
        populatePersistenceUnit(name);
    }

    @Override
    public void populateAllDatabases() {
        populatePersistenceUnits();
    }

    static class Holder {
        final Metadata metadata;
        final SessionFactoryImplementor sessionFactory;
        final ServiceRegistryImplementor serviceRegistry;

        Holder(Metadata metadata, SessionFactoryImplementor sessionFactory, ServiceRegistryImplementor serviceRegistry) {
            this.metadata = metadata;
            this.sessionFactory = sessionFactory;
            this.serviceRegistry = serviceRegistry;
        }
    }

    private static class SimpleExecutionOptions implements ExecutionOptions {
        private final Map<String, Object> configurationValues;

        public SimpleExecutionOptions(ServiceRegistry serviceRegistry) {
            configurationValues = serviceRegistry.getService(ConfigurationService.class).getSettings();
        }

        @Override
        public Map<String, Object> getConfigurationValues() {
            return configurationValues;
        }

        @Override
        public boolean shouldManageNamespaces() {
            return false;
        }

        @Override
        public ExceptionHandler getExceptionHandler() {
            return new ExceptionHandler() {
                @Override
                public void handleException(CommandAcceptanceException exception) {
                    log.error("Failed to recreate schema", exception);
                }
            };
        }
    }

}
