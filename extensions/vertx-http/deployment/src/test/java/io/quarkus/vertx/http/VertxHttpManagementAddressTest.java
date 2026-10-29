package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.runtime.Address;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class VertxHttpManagementAddressTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NoOpHandler.class, AddressServiceConsumer.class)
                    .addAsResource(new StringAsset("quarkus.management.enabled=true\n"), "application.properties"))
            // the management listener only starts when there is at least one management route
            .addBuildChainCustomizer(managementRoute())
            .addBuildChainCustomizer(AddressServiceConsumer.buildStep());

    @Inject
    @Named("vertx-http")
    Address main;

    @Inject
    @Named("vertx-management")
    Address management;

    @Test
    void injectMainAndManagement() {
        assertThat(main.uri()).hasToString("http://localhost:8081");
        // the management address carries the management root path, so references are relative to it
        assertThat(management.uri()).hasToString("http://localhost:9001/q");
    }

    @Test
    void requestAddressesFromServiceGraph() {
        // a service that declares request(Address.class, ...) receives the address of both bound listeners
        assertThat(AddressServiceConsumer.main).contains(main);
        assertThat(AddressServiceConsumer.management).contains(management);
    }

    @Test
    void resolveMainAndManagement() {
        assertThat(Address.resolve("quarkus://vertx-http").uri()).hasToString("http://localhost:8081");
        assertThat(Address.resolve("quarkus://vertx-management/health").uri())
                .hasToString("http://localhost:9001/q/health");
    }

    static Consumer<BuildChainBuilder> managementRoute() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                context.produce(buildItem.routeBuilder()
                        .management()
                        .route("test")
                        .handler(new NoOpHandler())
                        .blockingRoute()
                        .build());
            }
        }).produces(RouteBuildItem.class)
                .consumes(NonApplicationRootPathBuildItem.class)
                .build();
    }

    public static class NoOpHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext rc) {
            rc.response().end();
        }
    }
}
