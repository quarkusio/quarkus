package io.quarkus.arc.test.unproxyable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;

public class PrivateNoArgsConstructorDoNotChangeFlagTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PrivateNoArgsConstructorDoNotChangeFlagTest.class, MyBean.class)
                    .addAsResource(new StringAsset("quarkus.arc.transform-unproxyable-classes=false"),
                            "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void testValidationFailed() {
        Assertions.fail();
    }

    @Unremovable
    @ApplicationScoped
    public static class MyBean {

        private final String foo;

        // The private constructor should result in deployment exception
        private MyBean() {
            this.foo = "ok";
        }

        String ping() {
            return foo;
        }

    }

}
