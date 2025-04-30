package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class NamedRecordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Beans.class, ListProducer.class)
                    .addAsResource(
                            new StringAsset(
                                    "{#each cdi:beans.names}{it}::{/each}"),
                            "templates/foo.html"));

    @Inject
    Engine engine;

    @Test
    public void testResult() {
        assertEquals("Jachym::Vojtech::Ondrej::", engine.getTemplate("foo").render());
    }

    // @Singleton is added automatically
    @Named
    public record Beans(List<String> names) {
    }

    @Singleton
    public static class ListProducer {

        @Produces
        List<String> names() {
            return List.of("Jachym", "Vojtech", "Ondrej");
        }
    }

}
