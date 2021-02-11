package io.quarkus.qute.deployment.typesafe;

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

public class InterfaceValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset("{@java.util.List list}"
                            + "{list.empty}:{list.toString}"),
                            "templates/list.html"));

    @Inject
    Template list;

    @Test
    public void testResult() {
        assertEquals("true:[]", list.data("list", Collections.emptyList()).render());
    }

}
