package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractorInitiator;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

public final class QuarkusImportSqlCommandExtractorInitiator implements StandardServiceInitiator<ImportSqlCommandExtractor> {
    public static final ImportSqlCommandExtractorInitiator INSTANCE = new ImportSqlCommandExtractorInitiator();
    private static final MultipleLinesSqlCommandExtractor SERVICE_INSTANCE = new MultipleLinesSqlCommandExtractor();

    @Override
    public ImportSqlCommandExtractor initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return SERVICE_INSTANCE;
    }

    @Override
    public Class<ImportSqlCommandExtractor> getServiceInitiated() {
        return ImportSqlCommandExtractor.class;
    }
}
