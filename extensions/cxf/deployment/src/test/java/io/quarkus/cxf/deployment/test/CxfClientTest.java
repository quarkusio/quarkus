package io.quarkus.cxf.deployment.test;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class CxfClientTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(FruitWebService.class)
                            .addClass(Fruit.class)
                            .addAsResource("client.properties");
                }
            });

    @Inject
    FruitWebService clientService;

    @Test
    public void whenCheckingClient() {
        Assertions.assertEquals(2, clientService.count());
    }

}
