package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;

public class DuplicateIdentityExpressionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(DuplicateIdentityExpressionTest.InvalidBean.class)
                    .addAsResource(new StringAsset("my.identity=my_name"),
                            "application.properties"));

    @Test
    public void test() {
    }

    static class InvalidBean {

        @Scheduled(every = "1s", identity = "{my.identity}")
        @Scheduled(every = "1s", identity = "my_name")
        void wrong() {
        }

    }

}
