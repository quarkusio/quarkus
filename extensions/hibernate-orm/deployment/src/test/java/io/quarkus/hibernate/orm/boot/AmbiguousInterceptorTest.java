package io.quarkus.hibernate.orm.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Interceptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.test.QuarkusUnitTest;

public class AmbiguousInterceptorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(DefaultPUInterceptor.class)
                    .addClass(AnotherDefaultPUInterceptor.class))
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessageContainingAll("Multiple instances of Interceptor were found at ",
                            "io.quarkus.hibernate.orm.boot.AmbiguousInterceptorTest.DefaultPUInterceptor",
                            "io.quarkus.hibernate.orm.boot.AmbiguousInterceptorTest.AnotherDefaultPUInterceptor",
                            "for persistence unit <default>. At most one instance can be assigned to each persistence unit.")
                    .hasNoSuppressedExceptions());

    @PersistenceUnitExtension
    public static class DefaultPUInterceptor implements Interceptor {
    }

    @PersistenceUnitExtension
    public static class AnotherDefaultPUInterceptor implements Interceptor {
    }

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
