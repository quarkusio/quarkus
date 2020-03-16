package io.quarkus.cxf.deployment.test;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CxfClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addAsResource(new StringAsset(
                            "quarkus.cxf.endpoint.\"/fruit\".client-endpoint-url=http://localhost:8080/\nquarkus.cxf.endpoint.\"/fruit\".service-interface=io.quarkus.cxf.deployment.test.FruitWebService"),
                            "application.properties"));

    @Inject
    FruitWebService clientService;

    @Test
    public void whenCheckingClientInjected() {
        Assertions.assertNotNull(clientService);
    }

}
