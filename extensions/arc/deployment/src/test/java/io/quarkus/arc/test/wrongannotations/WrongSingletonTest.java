package io.quarkus.arc.test.wrongannotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class WrongSingletonTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EjbSingleton.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(rootCause.getMessage().contains("javax.ejb.Singleton"), t.toString());
                assertTrue(rootCause.getMessage().contains("com.google.inject.Singleton"), t.toString());
            });

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        fail();
    }

    @javax.ejb.Singleton
    static class EjbSingleton {

        @Inject
        @ConfigProperty(name = "unconfigured")
        String foo;

    }

    @ApplicationScoped
    static class GuiceProducers {

        @com.google.inject.Singleton
        @Produces
        List<String> produceEjbSingleton() {
            return Collections.emptyList();
        }

    }

}
