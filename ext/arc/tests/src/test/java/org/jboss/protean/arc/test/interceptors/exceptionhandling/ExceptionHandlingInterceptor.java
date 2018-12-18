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

package org.jboss.protean.arc.test.interceptors.exceptionhandling;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@ExceptionHandlingInterceptorBinding
@Priority(1)
@Interceptor
public class ExceptionHandlingInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        if (ctx.getParameters()[0] == ExceptionHandlingCase.OTHER_EXCEPTIONS) {
            throw new MyOtherException();
        }
        return ctx.proceed();
    }
}
