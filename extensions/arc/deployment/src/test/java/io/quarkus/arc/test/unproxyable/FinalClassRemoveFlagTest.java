package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FinalClassRemoveFlagTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FinalClassRemoveFlagTest.class, MyBean.class));

    @Inject
    MyBean bean;

    @Test
    public void testFinalFlagWasRemoved() {
        assertEquals("ok", bean.ping());
    }

    // The final flag should normally result in deployment exception
    @ApplicationScoped
    public static final class MyBean {

        private String foo;

        String ping() {
            return foo;
        }

        @PostConstruct
        void init() {
            foo = "ok";
        }

    }

}
