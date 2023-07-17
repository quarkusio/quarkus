package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FinalMethodRemoveFlagTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FinalMethodRemoveFlagTest.class, MyBean.class, MyParent.class));

    @Inject
    MyBean bean;

    @Test
    public void testFinalFlagWasRemoved() {
        assertEquals("ok", bean.ping());
        assertEquals("parent", bean.parentPing());
    }

    // The final flag should normally result in deployment exception
    @ApplicationScoped
    public static class MyBean extends MyParent {

        private String foo;

        final String ping() {
            return foo;
        }

        @PostConstruct
        void init() {
            foo = "ok";
        }

    }

    public static class MyParent {

        private String parent;

        final String parentPing() {
            return parent;
        }

        @PostConstruct
        void parentInit() {
            parent = "parent";
        }

    }
}
