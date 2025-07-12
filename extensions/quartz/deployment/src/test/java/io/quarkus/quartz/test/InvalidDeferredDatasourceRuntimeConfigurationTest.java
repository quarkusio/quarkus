package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import jakarta.enterprise.inject.CreationException;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidDeferredDatasourceRuntimeConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            // add a mock pretending to provide Agroal Capability to pass our validation
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(
                                    new CapabilityBuildItem(Capability.AGROAL, "fakeProvider"));
                        }
                    }).produces(CapabilityBuildItem.class).build();
                }
            })
            .assertException(t -> {
                assertEquals(CreationException.class, t.getClass());
                Assertions.assertTrue(t.getMessage().contains(
                        "Deferred datasource name is missing - you can configure it via quarkus.quartz.deferred-datasource-name"));
            })
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleJobs.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.defer-datasource-check=true\n"
                                    + "quarkus.quartz.store-type=jdbc-cmt"),
                            "application.properties"));

    @Test
    public void shouldFailAndNotReachHere() {
        Assertions.fail();
    }
}
