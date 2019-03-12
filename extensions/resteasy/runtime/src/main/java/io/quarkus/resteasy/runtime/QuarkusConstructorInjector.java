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

package io.quarkus.resteasy.runtime;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.WebApplicationException;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import io.quarkus.arc.runtime.BeanContainer;

public class QuarkusConstructorInjector implements ConstructorInjector {

    private volatile BeanContainer.Factory<?> factory;

    private final ConstructorInjector delegate;

    private final Constructor<?> ctor;

    public QuarkusConstructorInjector(Constructor<?> ctor, ConstructorInjector delegate) {
        this.ctor = ctor;
        this.delegate = delegate;
    }

    @Override
    public CompletionStage<Object> construct(boolean unwrapAsync) {
        if (QuarkusInjectorFactory.CONTAINER == null) {
            return this.delegate.construct(unwrapAsync);
        }
        if (factory == null) {
            factory = QuarkusInjectorFactory.CONTAINER.instanceFactory(this.ctor.getDeclaringClass());
        }
        if (factory == null) {
            return delegate.construct(unwrapAsync);
        }
        return CompletableFuture.completedFuture(factory.create().get());
    }

    @Override
    public CompletionStage<Object> construct(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure, WebApplicationException, ApplicationException {
        if (QuarkusInjectorFactory.CONTAINER == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        if (factory == null) {
            factory = QuarkusInjectorFactory.CONTAINER.instanceFactory(this.ctor.getDeclaringClass());
        }
        if (factory == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        return CompletableFuture.completedFuture(factory.create().get());
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
