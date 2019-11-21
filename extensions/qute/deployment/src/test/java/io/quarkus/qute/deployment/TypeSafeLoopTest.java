package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class TypeSafeLoopTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Foo.class)
                    .addAsResource(new StringAsset("{@java.util.List<io.quarkus.qute.deployment.Foo> list}"
                            + "{#for foo in list}"
                            + "{foo.name}={foo.age}={foo.charlie.name}"
                            + "{/}"), "META-INF/resources/templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testValidation() {
        assertEquals("bravo=10=BRAVO",
                foo.data("list", Collections.singletonList(new Foo("bravo", 10l))).render());
    }

}
