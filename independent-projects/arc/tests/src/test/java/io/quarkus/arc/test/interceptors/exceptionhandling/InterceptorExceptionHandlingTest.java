package io.quarkus.arc.test.interceptors.exceptionhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InterceptorExceptionHandlingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ExceptionHandlingBean.class,
            ExceptionHandlingInterceptor.class, ExceptionHandlingInterceptorBinding.class);

    @Test
    public void testProperlyThrowsDeclaredExceptions() throws MyDeclaredException {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        assertThrows(MyDeclaredException.class, () -> exceptionHandlingBean.foo(ExceptionHandlingCase.DECLARED_EXCEPTION));
    }

    @Test
    public void testProperlyThrowsRuntimeExceptions() throws MyDeclaredException {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        assertThrows(MyRuntimeException.class, () -> exceptionHandlingBean.foo(ExceptionHandlingCase.RUNTIME_EXCEPTION));
    }

    @Test
    public void testWrapsOtherExceptions() throws MyDeclaredException {
        try {
            ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

            assertTrue(exceptionHandlingBean instanceof Subclass);

            exceptionHandlingBean.foo(ExceptionHandlingCase.OTHER_EXCEPTIONS);
            fail("The method should have thrown a RuntimeException wrapping a MyOtherException but didn't throw any exception.");
        } catch (RuntimeException e) {
            // Let's check the cause is consistent with what we except.
            assertEquals(MyOtherException.class, e.getCause().getClass());
        } catch (Exception e) {
            fail("The method should have thrown a RuntimeException wrapping a MyOtherException but threw: " + e);
        }
    }

    @Test
    public void testThrowsException() throws Exception {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        assertThrows(Exception.class, () -> exceptionHandlingBean.bar());
    }

    @Test
    public void testThrowsRuntimeException() {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        assertThrows(RuntimeException.class, () -> exceptionHandlingBean.baz());
    }
}
