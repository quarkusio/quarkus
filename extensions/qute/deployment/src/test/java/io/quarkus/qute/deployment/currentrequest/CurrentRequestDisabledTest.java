package io.quarkus.qute.deployment.currentrequest;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class CurrentRequestDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.exclude-types=io.quarkus.vertx.http.runtime.CurrentRequestProducer"),
                            "application.properties")
                    .addAsResource(new StringAsset(
                            "Hello {inject:vertxRequest.getParam('name')}!"),
                            "templates/request.txt"))
            .setExpectedException(TemplateException.class);

    @Test
    public void testCurrentRequest() {
        fail();
    }

}
