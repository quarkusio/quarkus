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

package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.Rule;
import org.junit.Test;

public class AroundConstructReturningObjectTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class);

    public static AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        assertNotNull(simpleBean);
        assertTrue(INTERCEPTOR_CALLED.get());
    }

    @Singleton
    @MyTransactional
    static class SimpleBean {

    }

    @MyTransactional
    @Interceptor
    public static class SimpleInterceptor {

        @AroundConstruct
        Object mySuperCoolAroundConstruct(InvocationContext ctx) throws Exception {
            INTERCEPTOR_CALLED.set(true);
            return ctx.proceed();
        }

    }
}
