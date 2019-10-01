package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

import io.quarkus.test.QuarkusUnitTest;

public class XaRequiresXaDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-wrongdriverkind-datasource.properties", "application.properties"))
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
            });

    @Test
    public void xaRequiresJta() {
        //Should not be reached: verify
        Assert.assertTrue(false);
    }

}
