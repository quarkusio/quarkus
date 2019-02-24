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

import javax.enterprise.context.Dependent;

@Dependent
public class ExceptionHandlingBean {

    public ExceptionHandlingBean() {
    }

    @ExceptionHandlingInterceptorBinding
    void foo(ExceptionHandlingCase exceptionHandlingCase) throws MyDeclaredException {
        switch (exceptionHandlingCase) {
        case DECLARED_EXCEPTION:
            throw new MyDeclaredException();
        case RUNTIME_EXCEPTION:
            throw new MyRuntimeException();
        case OTHER_EXCEPTIONS:
            // this case should be handled by the interceptor
            break;
        }
    }

    @ExceptionHandlingInterceptorBinding
    void bar() throws Exception {
        throw new Exception();
    }

    @ExceptionHandlingInterceptorBinding
    void baz() throws RuntimeException {
        throw new RuntimeException();
    }

    Integer fooNotIntercepted() {
        return 1;
    }
}
