package io.quarkus.qute.deployment.altexprcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class DependencyWithQuteConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("Hello {name}!"), "templates/hello.html"))
            .withAdditionalDependency(dependency -> dependency
                    .addAsResource(new StringAsset("alt-expr-syntax = true"), "templates/.qute")
                    .addAsResource(new StringAsset("Hi {=name}!"), "templates/hi.html"));

    @Inject
    Template hello;

    @Inject
    Template hi;

    @Test
    public void testScanning() {
        assertEquals("Hello Lina!", hello.data("name", "Lina").render());
        assertEquals("Hi Lina!", hi.data("name", "Lina").render());
    }

}
