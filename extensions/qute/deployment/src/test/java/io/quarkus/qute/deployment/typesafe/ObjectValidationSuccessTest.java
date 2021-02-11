package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ObjectValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset("{@java.lang.Object obj}"
                            + "{@java.lang.Object anotherObj}"
                            + "{obj.toString}:{anotherObj.raw}"),
                            "templates/object.html"));

    @Inject
    Template object;

    @Test
    public void testResult() {
        assertEquals("hello:<strong>", object.data("obj", "hello").data("anotherObj", "<strong>").render());
    }

}
