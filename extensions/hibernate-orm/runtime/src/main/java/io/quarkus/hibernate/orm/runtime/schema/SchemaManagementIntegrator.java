package io.quarkus.hibernate.orm.runtime.schema;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
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
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        String name = defaultName(sessionFactory);
        metadataMap.put(name, new Holder(metadata, sessionFactory, serviceRegistry));
        nameCache.put(sessionFactory, name);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        metadataMap.remove(nameCache.get(sessionFactory));
        nameCache.remove(sessionFactory);
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
        if (nameCache.containsKey(name)) {
            return nameCache.get(name);
        }
        Object prop = sf.getProperties().get("hibernate.ejb.persistenceUnitName");
        if (prop != null) {
            return prop.toString();
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
        Holder val = metadataMap.get(name);

        Object prop = val.sessionFactory.getProperties().get("javax.persistence.schema-generation.database.action");
        if (prop != null && !(prop.toString().equals("none"))) {
            //if this is none we assume another framework is doing this (e.g. flyway)
            SchemaManagementTool schemaManagementTool = val.sessionFactory.getServiceRegistry()
                    .getService(SchemaManagementTool.class);
            SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(new HashMap());
            schemaDropper
                    .doDrop(val.metadata, new SimpleExecutionOptions(), new SimpleSourceDescriptor(),
                            new SimpleTargetDescriptor());
            schemaManagementTool.getSchemaCreator(new HashMap())
                    .doCreation(val.metadata, new SimpleExecutionOptions(), new SimpleSourceDescriptor(),
                            new SimpleTargetDescriptor());
        }
        //we still clear caches though
        val.sessionFactory.getCache().evictAll();
        val.sessionFactory.getCache().evictQueries();
    }

    @Override
    public void resetDatabase(String dbName) {
        String name = datasourceToPuMap.get(dbName);
        if (name == null) {
            //not a hibernate DS
            return;
        }
        recreateDatabase(name);
    }

    @Override
    public void resetAllDatabases() {
        recreateDatabases();
    }

    static class Holder {
        final Metadata metadata;
        final SessionFactoryImplementor sessionFactory;
        final SessionFactoryServiceRegistry serviceRegistry;

        Holder(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
            this.metadata = metadata;
            this.sessionFactory = sessionFactory;
            this.serviceRegistry = serviceRegistry;
        }
    }

    private static class SimpleExecutionOptions implements ExecutionOptions {
        @Override
        public Map getConfigurationValues() {
            return Collections.emptyMap();
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

    private static class SimpleSourceDescriptor implements SourceDescriptor {
        @Override
        public SourceType getSourceType() {
            return SourceType.METADATA;
        }

        @Override
        public ScriptSourceInput getScriptSourceInput() {
            return null;
        }
    }

    private static class SimpleTargetDescriptor implements TargetDescriptor {
        @Override
        public EnumSet<TargetType> getTargetTypes() {
            return EnumSet.of(TargetType.DATABASE);
        }

        @Override
        public ScriptTargetOutput getScriptTargetOutput() {
            return null;
        }
    }
}
