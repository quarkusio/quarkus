package io.quarkus.liquibase.runtime.devconsole;

import java.util.List;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.LiquibaseContainer;
import io.quarkus.liquibase.runtime.LiquibaseContainerSupplier;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.devconsole.DevConsolePostHandler;
import io.quarkus.vertx.http.runtime.devmode.devconsole.FlashScopeUtil.FlashMessageStatus;
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
                List<LiquibaseContainer> liquibaseContainers = new LiquibaseContainerSupplier().get();
                for (LiquibaseContainer liquibaseContainer : liquibaseContainers) {
                    if (liquibaseContainer.getDataSourceName().equals(datasource)) {
                        LiquibaseFactory liquibaseFactory = liquibaseContainer.getLiquibaseFactory();
                        if ("clean".equals(operation)) {
                            try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                                liquibase.dropAll();
                            }
                            flashMessage(event, "Data source " + datasource + " cleaned");
                            return;
                        } else if ("migrate".equals(operation)) {
                            try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                                liquibase.update(liquibaseFactory.createContexts(),
                                        liquibaseFactory.createLabels());
                            }
                            flashMessage(event, "Data source " + datasource + " migrated");
                            return;
                        } else {
                            flashMessage(event, "Invalid operation: " + operation, FlashMessageStatus.ERROR);
                            return;
                        }
                    }
                }
                flashMessage(event, "Unknown datasource: " + datasource, FlashMessageStatus.ERROR);
            }
        };
    }
}
