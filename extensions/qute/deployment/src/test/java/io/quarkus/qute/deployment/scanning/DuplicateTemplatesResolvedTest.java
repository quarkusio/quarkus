package io.quarkus.qute.deployment.scanning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class DuplicateTemplatesResolvedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("Hello!"), "templates/hello.html"))
            .withAdditionalDependency(
                    d -> d.addAsResource(new StringAsset("Hi!"), "templates/hello.html"));

    @Inject
    Template hello;

    @Test
    public void testHello() {
        // Root archive takes precedence
        assertEquals("Hello!", hello.render());
    }

}
