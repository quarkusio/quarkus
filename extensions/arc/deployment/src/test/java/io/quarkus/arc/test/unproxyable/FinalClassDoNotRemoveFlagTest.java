package io.quarkus.arc.test.unproxyable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;

public class FinalClassDoNotRemoveFlagTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FinalClassDoNotRemoveFlagTest.class, MyBean.class)
                    .addAsResource(new StringAsset("quarkus.arc.transform-unproxyable-classes=false"),
                            "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        Assertions.fail();
    }

    // The final flag should result in deployment exception
    @Unremovable
    @ApplicationScoped
    public static final class MyBean {

        String ping() {
            return "ok";
        }

    }

}
