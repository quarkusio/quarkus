/*
 * Copyright 2013 Martin Kouba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.qute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Martin Kouba
 */
class MethodWrapper implements MemberWrapper {

    private final Method method;

    MethodWrapper(Method method) {
        super();
        this.method = method;
    }

    @Override
    public Object getValue(Object instance) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return method.invoke(instance);
    }

}
