package io.quarkus.datasource.runtime;

import java.util.ServiceLoader;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DatabaseRecorder {

    public Handler<RoutingContext> devConsoleResetDatabaseHandler() {
        // the usual issue of Vert.x hanging on to the first TCCL and setting it on all its threads
        final ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        return new DevConsolePostHandler() {

            @Override
            protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
                String name = form.get("name");
                ServiceLoader<DatabaseSchemaProvider> dbs = ServiceLoader.load(DatabaseSchemaProvider.class,
                        Thread.currentThread().getContextClassLoader());
                for (DatabaseSchemaProvider i : dbs) {
                    i.resetDatabase(name);
                }
                flashMessage(event, "Database successfully reset");
            }
        };
    }
}
