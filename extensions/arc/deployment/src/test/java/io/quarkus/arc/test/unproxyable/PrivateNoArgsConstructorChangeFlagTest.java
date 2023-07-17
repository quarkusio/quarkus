package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PrivateNoArgsConstructorChangeFlagTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PrivateNoArgsConstructorChangeFlagTest.class, MyBean.class));

    @Inject
    MyBean bean;

    @Test
    public void testFinalFlagWasRemoved() {
        assertEquals("ok", bean.ping());
    }

    @ApplicationScoped
    public static class MyBean {

        private final String foo;

        // The private constructor should normally result in deployment exception
        private MyBean() {
            this.foo = "ok";
        }

        String ping() {
            return foo;
        }

    }

}
