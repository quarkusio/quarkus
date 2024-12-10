package io.quarkus.datasource.runtime.dev.ui;

import java.util.ServiceLoader;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;

@ApplicationScoped
public class DatasourceJsonRpcService {

    public boolean reset(String ds) {
        ServiceLoader<DatabaseSchemaProvider> dbs = ServiceLoader.load(DatabaseSchemaProvider.class,
                Thread.currentThread().getContextClassLoader());
        for (DatabaseSchemaProvider i : dbs) {
            i.resetDatabase(ds);
        }
        return true;
    }
}
