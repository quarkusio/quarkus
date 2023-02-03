package io.quarkus.liquibase.runtime.devconsole;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.Default;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil.FlashMessageStatus;
import io.quarkus.liquibase.LiquibaseDataSource.LiquibaseDataSourceLiteral;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import liquibase.Liquibase;

@Recorder
public class LiquibaseDevConsoleRecorder {

    public Handler<RoutingContext> handler() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String datasource = form.get("datasource");
                String operation = form.get("operation");

                InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container().select(LiquibaseFactory.class);
                if (liquibaseFactoryInstance.isUnsatisfied()) {
                    return;
                }

                Annotation qualifier;
                if (DataSourceUtil.isDefault(datasource)) {
                    qualifier = Default.Literal.INSTANCE;
                } else {
                    qualifier = LiquibaseDataSourceLiteral.of(datasource);
                }

                InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle = Arc.container().instance(LiquibaseFactory.class,
                        qualifier);
                if (!liquibaseFactoryHandle.isAvailable()) {
                    flashMessage(event, "Unknown datasource: " + datasource, FlashMessageStatus.ERROR);
                }

                LiquibaseFactory liquibaseFactory = liquibaseFactoryHandle.get();
                if ("clean".equals(operation)) {
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        liquibase.dropAll();
                    }
                    flashMessage(event, "Datasource " + datasource + " cleaned");
                    return;
                } else if ("migrate".equals(operation)) {
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        liquibase.update(liquibaseFactory.createContexts(),
                                liquibaseFactory.createLabels());
                    }
                    flashMessage(event, "Datasource " + datasource + " migrated");
                    return;
                } else {
                    flashMessage(event, "Invalid operation: " + operation, FlashMessageStatus.ERROR);
                    return;
                }
            }
        };
    }
}
