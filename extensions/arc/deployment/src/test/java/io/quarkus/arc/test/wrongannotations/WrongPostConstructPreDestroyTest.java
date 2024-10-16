package io.quarkus.arc.test.wrongannotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class WrongPostConstructPreDestroyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MySingleton.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(rootCause.getMessage().contains("javax.annotation.PostConstruct"), t.toString());
                assertTrue(rootCause.getMessage().contains("javax.annotation.PreDestroy"), t.toString());
            });

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        fail();
    }

    @Singleton
    static class MySingleton {

        @PostConstruct
        void init() {
        }

        @PreDestroy
        void destroy() {
        }

    }

}
