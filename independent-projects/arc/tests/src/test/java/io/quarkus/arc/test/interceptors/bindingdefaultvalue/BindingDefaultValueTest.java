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

package io.quarkus.arc.test.interceptors.bindingdefaultvalue;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.Rule;
import org.junit.Test;

public class BindingDefaultValueTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class, AlphaInterceptor.class,
            BravoInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        assertEquals("foo::alpha", simpleBean.ping());
    }

    @Singleton
    static class SimpleBean {

        @MyTransactional // This should only match AlphaInterceptor
        String ping() {
            return "foo";
        }

    }

    @MyTransactional("alpha")
    @Priority(1)
    @Interceptor
    public static class AlphaInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed() + "::alpha";
        }
    }

    @MyTransactional("bravo")
    @Priority(2)
    @Interceptor
    public static class BravoInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed() + "::bravo";
        }
    }

}
