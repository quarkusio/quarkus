package io.quarkus.arc.test.observer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.AsyncObserverExceptionHandler;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class AmbiguousAsyncObserverExceptionHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MyFirstAsyncObserverExceptionHandler.class, MySecondAsyncObserverExceptionHandler.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(rootCause instanceof AmbiguousResolutionException);
            });

    @Test
    public void testValidationFailed() {
        fail();
    }

    @Singleton
    static class MyFirstAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

        @Override
        public void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
        }

    }

    @Singleton
    static class MySecondAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

        @Override
        public void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
        }

    }

}
