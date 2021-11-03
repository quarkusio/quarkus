package io.quarkus.qute.deployment.scanning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleTemplatesDirectoryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("Hello!"), "templates/hello.html"))
            .withAdditionalDependency(d -> d.addAsResource(new StringAsset("Hi!"), "templates/hi.html"));

    @Inject
    Template hello;

    @Inject
    Template hi;

    @Test
    public void testScanning() {
        assertEquals("Hello!", hello.render());
        assertEquals("Hi!", hi.render());
    }

}
