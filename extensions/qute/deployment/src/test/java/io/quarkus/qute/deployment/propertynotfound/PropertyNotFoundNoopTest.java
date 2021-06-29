package io.quarkus.qute.deployment.propertynotfound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class PropertyNotFoundNoopTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("foos:{foos}"), "templates/test.html")
                    .addAsResource(new StringAsset("quarkus.qute.property-not-found-strategy=noop"
                            + "\nquarkus.qute.strict-rendering=false"),
                            "application.properties"));

    @Inject
    Template test;

    @Test
    public void testNoop() {
        assertEquals("foos:", test.render());
    }

}
