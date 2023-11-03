package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestExtension;

@ExtendWith({ GetExceptionsInTestResourceTestCase.IgnoreCustomExceptions.class, QuarkusTestExtension.class })
@QuarkusTestResource(restrictToAnnotatedClass = true, value = GetExceptionsInTestResourceTestCase.KeepContextTestResource.class)
public class GetExceptionsInTestResourceTestCase {

    public static final AtomicReference<QuarkusTestResourceLifecycleManager.Context> CONTEXT = new AtomicReference<>();

    @BeforeEach
    public void setup() {
        throw new CustomException();
    }

    @Test
    public void testExceptionHaveBeenCaptured() {
        assertTrue(CONTEXT.get().getTestStatus().isTestFailed(), "Test didn't fail!");
        assertTrue(isCustomException(CONTEXT.get().getTestStatus().getTestErrorCause()));
    }

    private static boolean isCustomException(Throwable ex) {
        try {
            return ex.getClass().getClassLoader().loadClass(CustomException.class.getName()).isInstance(ex);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static class CustomException extends RuntimeException {

    }

    public static class IgnoreCustomExceptions implements LifecycleMethodExecutionExceptionHandler {
        @Override
        public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable)
                throws Throwable {
            if (!isCustomException(throwable)) {
                throw throwable;
            }
        }
    }

    public static class KeepContextTestResource implements QuarkusTestResourceLifecycleManager {
        @Override
        public void setContext(Context context) {
            CONTEXT.set(context);
        }

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void stop() {
            // do nothing
        }
    }
}
