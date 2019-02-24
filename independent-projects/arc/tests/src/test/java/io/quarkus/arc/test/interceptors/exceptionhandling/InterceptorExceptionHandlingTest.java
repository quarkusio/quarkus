/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.test.interceptors.exceptionhandling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class InterceptorExceptionHandlingTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(ExceptionHandlingBean.class,
            ExceptionHandlingInterceptor.class, ExceptionHandlingInterceptorBinding.class);

    @Test(expected = MyDeclaredException.class)
    public void testProperlyThrowsDeclaredExceptions() throws MyDeclaredException {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        exceptionHandlingBean.foo(ExceptionHandlingCase.DECLARED_EXCEPTION);
    }

    @Test(expected = MyRuntimeException.class)
    public void testProperlyThrowsRuntimeExceptions() throws MyDeclaredException {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        exceptionHandlingBean.foo(ExceptionHandlingCase.RUNTIME_EXCEPTION);
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

    @Test(expected = Exception.class)
    public void testThrowsException() throws Exception {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        exceptionHandlingBean.bar();
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsRuntimeException() {
        ExceptionHandlingBean exceptionHandlingBean = Arc.container().instance(ExceptionHandlingBean.class).get();

        assertTrue(exceptionHandlingBean instanceof Subclass);

        exceptionHandlingBean.baz();
    }
}
