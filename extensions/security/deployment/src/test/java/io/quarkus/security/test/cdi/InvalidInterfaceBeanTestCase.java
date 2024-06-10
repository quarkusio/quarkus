package io.quarkus.security.test.cdi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.cdi.app.interfaces.InvalidBeanWithInterface;
import io.quarkus.security.test.cdi.app.interfaces.InvalidInterface1;
import io.quarkus.security.test.cdi.app.interfaces.InvalidInterface2;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidInterfaceBeanTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot(
                    (jar) -> jar.addClasses(InvalidBeanWithInterface.class,
                            InvalidInterface1.class,
                            InvalidInterface2.class));

    @Test
    public void testRejected() {
        Assertions.fail();
    }
}
