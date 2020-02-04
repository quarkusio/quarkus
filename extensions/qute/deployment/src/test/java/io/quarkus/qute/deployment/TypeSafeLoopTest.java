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
                    .addClasses(Foo.class, MyFooList.class)
                    .addAsResource(new StringAsset("{@java.util.List<io.quarkus.qute.deployment.Foo> list}"
                            + "{@io.quarkus.qute.deployment.MyFooList fooList}"
                            + "{#for foo in list}"
                            + "{foo.name}={foo.age}={foo.charlie.name}"
                            + "{/}"
                            + "::"
                            + "{#for foo in fooList}"
                            + "{foo.name}={foo.age}={foo.charlie.name}"
                            + "{/}"
                            + "::"
                            + "{fooList.get(0).name}"), "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testValidation() {
        assertEquals("bravo=10=BRAVO::alpha=1=ALPHA::alpha",
                foo.data("list", Collections.singletonList(new Foo("bravo", 10l)))
                        .data("fooList", new MyFooList(new Foo("alpha", 1l))).render());
    }

}
