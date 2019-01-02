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

package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.WebApplicationException;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.shamrock.runtime.cdi.BeanContainer;

public class ShamrockConstructorInjector implements ConstructorInjector {

    private volatile BeanContainer.Factory<?> factory;

    private final ConstructorInjector delegate;

    private final Constructor<?> ctor;

    public ShamrockConstructorInjector(Constructor<?> ctor, ConstructorInjector delegate) {
        this.ctor = ctor;
        this.delegate = delegate;
    }

    @Override
    public CompletionStage<Object> construct(boolean unwrapAsync) {
        return this.delegate.construct(unwrapAsync);
    }

    @Override
    public CompletionStage<Object> construct(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure, WebApplicationException, ApplicationException {
        if (ShamrockInjectorFactory.CONTAINER == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        if(factory == null) {
            factory = ShamrockInjectorFactory.CONTAINER.instanceFactory(this.ctor.getDeclaringClass());
        }
        if(factory == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        return CompletableFuture.completedFuture(factory.get());
    }

    @Override
    public CompletionStage<Object[]> injectableArguments(boolean unwrapAsync) {
        return this.delegate.injectableArguments(unwrapAsync);
    }

    @Override
    public CompletionStage<Object[]> injectableArguments(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure {
        return this.delegate.injectableArguments(request, response, unwrapAsync);
    }
}
