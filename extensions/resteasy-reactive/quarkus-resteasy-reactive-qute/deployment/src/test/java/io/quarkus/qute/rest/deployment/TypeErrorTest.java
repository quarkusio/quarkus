package io.quarkus.qute.rest.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TypeErrorTest {

    @RegisterExtension
    static final QuarkusUnitTest configError = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class)
                    .addAsResource("templates/HelloResource/hello.txt")
                    .addAsResource("templates/HelloResource/typeError.txt")
                    .addAsResource("templates/HelloResource/typedTemplate.txt")
                    .addAsResource("templates/HelloResource/typedTemplatePrimitives.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/hello.txt"))
            .assertException(t -> {
                assertTrue(t.getMessage().contains("Incorrect expression"));
                assertTrue(t.getMessage().contains("name.foo()"));
            });

    @Test
    public void emptyTest() {
    }
}
