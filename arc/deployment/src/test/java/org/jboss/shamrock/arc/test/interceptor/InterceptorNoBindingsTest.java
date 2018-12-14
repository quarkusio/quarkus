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

package org.jboss.shamrock.arc.test.interceptor;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.DefinitionException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.shamrock.test.BuildShouldFailWith;
import org.jboss.shamrock.test.Deployment;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockUnitTest.class)
public class InterceptorNoBindingsTest {

    @BuildShouldFailWith(DefinitionException.class)
    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(InterceptorWithoutBindings.class);
    }

    @Test
    public void testDeploymentFailed() {
        // This method should not be invoked
    }

    @Priority(1)
    @Interceptor
    static class InterceptorWithoutBindings {

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }

    }

}
