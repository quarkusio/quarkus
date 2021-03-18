package io.quarkus.agroal.runtime.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class CleanDatabaseInterceptor {

    final List<DatabaseSchemaProvider> providers;

    public CleanDatabaseInterceptor() {
        this.providers = new ArrayList<>();
        ServiceLoader<DatabaseSchemaProvider> dbs = ServiceLoader.load(DatabaseSchemaProvider.class,
                Thread.currentThread().getContextClassLoader());
        for (DatabaseSchemaProvider i : dbs) {
            providers.add(i);
        }
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        try {
            return context.proceed();
        } finally {
            for (DatabaseSchemaProvider i : providers) {
                i.resetAllDatabases();
            }
        }
    }
}
