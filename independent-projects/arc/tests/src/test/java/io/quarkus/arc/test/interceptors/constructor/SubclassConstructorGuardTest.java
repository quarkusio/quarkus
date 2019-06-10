/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.arc.test.interceptors.constructor;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.Rule;
import org.junit.Test;

public class SubclassConstructorGuardTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            SimpleInterceptor.class);

    @Test
    public void testConstructorGuard() throws IOException {
        assertEquals("foo::bar", Arc.container().instance(SimpleBean.class).get().foo());
    }

    @Simple
    @Singleton
    static class SimpleBean {

        private String val;

        public SimpleBean() {
            init();
        }

        void init() {
            this.val = "bar";
        }

        String foo() {
            return val;
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return "foo" + "::" + ctx.proceed();
        }
    }

}
