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

package io.quarkus.arc.test.interceptors.bindings;

import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InvocationContextImpl;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.Rule;
import org.junit.Test;

public class InvocationContextBindingsTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Simple.class, MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        // [@io.quarkus.arc.test.interceptors.Simple(),
        // @io.quarkus.arc.test.interceptors.bindings.MyTransactional(value={java.lang.String.class})]::foo
        String ret = simpleBean.foo();
        assertTrue(ret.contains(Simple.class.getName()));
        assertTrue(ret.contains(MyTransactional.class.getName()));
        assertTrue(ret.contains(String.class.getName()));
    }

    @Singleton
    static class SimpleBean {

        @MyTransactional({ String.class })
        @Simple
        String foo() {
            return "foo";
        }

    }

    @Simple
    @MyTransactional
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            Object bindings = ctx.getContextData().get(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS);
            if (bindings != null) {
                return bindings.toString() + "::" + ctx.proceed();
            }
            return ctx.proceed();
        }
    }

}
