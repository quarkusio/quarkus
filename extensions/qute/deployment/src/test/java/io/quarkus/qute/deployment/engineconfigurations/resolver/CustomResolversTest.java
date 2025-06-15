package io.quarkus.qute.deployment.engineconfigurations.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.ValueResolver;
import io.quarkus.test.QuarkusUnitTest;

public class CustomResolversTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            root -> root.addClasses(CustomValueResolver.class, CustomNamespaceResolver.class, ResolverBase.class)
                    .addAsResource(new StringAsset("{bool.foo}::{custom:bar}"), "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testResolvers() {
        assertEquals("foo::foo", foo.data("bool", true).render());
    }

    @EngineConfiguration
    static class CustomValueResolver extends ResolverBase {

        @Override
        public boolean appliesTo(EvalContext context) {
            return context.getBase() instanceof Boolean;
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return CompletableFuture.completedStage("foo");
        }

    }

    static abstract class ResolverBase implements ValueResolver {

    }

    @EngineConfiguration
    static class CustomNamespaceResolver implements NamespaceResolver {

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return CompletableFuture.completedStage("foo");
        }

        @Override
        public String getNamespace() {
            return "custom";
        }

    }

}
