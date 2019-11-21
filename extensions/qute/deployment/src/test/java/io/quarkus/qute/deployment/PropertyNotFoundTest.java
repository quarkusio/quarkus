package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PropertyNotFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Foo.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.PropertyNotFoundTest$Foo foo}"
                            + "{foo.surname}"), "META-INF/resources/templates/foo.html"))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

    static class Foo {

        public String name;

        public Long age;

    }

}
