package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

public final class QuarkusImportSqlCommandExtractorInitiator implements StandardServiceInitiator<SqlScriptCommandExtractor> {

    public static final QuarkusImportSqlCommandExtractorInitiator INSTANCE = new QuarkusImportSqlCommandExtractorInitiator();

    private static final MultiLineSqlScriptExtractor SERVICE_INSTANCE = new MultiLineSqlScriptExtractor();

    @Override
    public SqlScriptCommandExtractor initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return SERVICE_INSTANCE;
    }

    @Override
    public Class<SqlScriptCommandExtractor> getServiceInitiated() {
        return SqlScriptCommandExtractor.class;
    }
}
