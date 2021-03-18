package io.quarkus.agroal.runtime;

import java.util.ServiceLoader;
import java.util.function.Supplier;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.schema.DatabaseSchemaProvider;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class AgroalRecorder {

    public Supplier<DataSourceSupport> dataSourceSupportSupplier(DataSourceSupport dataSourceSupport) {
        return new Supplier<DataSourceSupport>() {
            @Override
            public DataSourceSupport get() {
                return dataSourceSupport;
            }
        };
    }

    public Supplier<AgroalDataSource> agroalDataSourceSupplier(String dataSourceName,
            @SuppressWarnings("unused") DataSourcesRuntimeConfig dataSourcesRuntimeConfig) {
        final AgroalDataSource agroalDataSource = DataSources.fromName(dataSourceName);
        return new Supplier<AgroalDataSource>() {
            @Override
            public AgroalDataSource get() {
                return agroalDataSource;
            }
        };
    }

    public Handler<RoutingContext> devConsoleCleanDatabaseHandler() {
        // the usual issue of Vert.x hanging on to the first TCCL and setting it on all its threads
        final ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String name = form.get("name");
                ServiceLoader<DatabaseSchemaProvider> dbs = ServiceLoader.load(DatabaseSchemaProvider.class,
                        Thread.currentThread().getContextClassLoader());
                for (DatabaseSchemaProvider i : dbs) {
                    i.resetDatabase(name);
                }
                flashMessage(event, "Action invoked");
            }
        };
    }
}
