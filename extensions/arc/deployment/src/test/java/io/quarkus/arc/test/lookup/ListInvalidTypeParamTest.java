package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ListInvalidTypeParamTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Foo.class)).assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(rootCause instanceof DeploymentException);
                String suppressedMessage = Arrays.stream(rootCause.getSuppressed()).map(Throwable::getMessage)
                        .collect(Collectors.joining("::"));
                assertTrue(suppressedMessage.contains("Type variable is not a legal type argument"), rootCause.toString());
                assertTrue(suppressedMessage.contains("Wildcard is not a legal type argument"), rootCause.toString());
            });

    @Test
    public void testFailure() {
        fail();
    }

    @Singleton
    static class Foo<T> {

        @Inject
        @All
        List<T> services;

        @All
        List<?> counters;

    }

}
