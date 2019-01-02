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

package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;

/**
 * Represents an injectable interceptor bean. It is an alternative to {@link javax.enterprise.inject.spi.Interceptor}.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InjectableInterceptor<T> extends InjectableBean<T> {

    /**
     *
     * @return the interceptor bindings
     */
    Set<Annotation> getInterceptorBindings();

    /**
     *
     * @param type
     * @return {@code true} if this interceptor intercepts the given kind of interception, {@code false} othewise
     */
    boolean intercepts(InterceptionType type);

    /**
     *
     * @param type
     * @param instance
     * @param ctx
     * @return the invocation return value
     * @throws Exception
     */
    Object intercept(InterceptionType type, T instance, InvocationContext ctx) throws Exception;

    /**
     *
     * @return the priority value
     */
    int getPriority();

}
