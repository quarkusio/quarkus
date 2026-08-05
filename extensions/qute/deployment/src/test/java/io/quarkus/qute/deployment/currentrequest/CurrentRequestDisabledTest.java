package io.quarkus.qute.deployment.currentrequest;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusExtensionTest;

public class CurrentRequestDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
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
