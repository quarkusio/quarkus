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

package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;

/**
 * Represents a container instance.
 * 
 * @author Martin Kouba
 */
public interface ArcContainer {

    /**
     * Unlike {@link BeanManager#getContext(Class)} this method does not throw
     * {@link javax.enterprise.context.ContextNotActiveException} if there is no active context for the given
     * scope.
     * 
     * @param scopeType
     * @return the active context or null
     * @throws IllegalArgumentException if there is more than one active context for the given scope
     */
    InjectableContext getActiveContext(Class<? extends Annotation> scopeType);

    /**
     * 
     * @return the set of all supported scopes
     */
    Set<Class<? extends Annotation>> getScopes();

    /**
     * Never returns null. However, the handle is empty if no bean matches/multiple beans match the specified type and
     * qualifiers.
     *
     * @param type
     * @param qualifiers
     * @return a new instance handle
     */
    <T> InstanceHandle<T> instance(Class<T> type, Annotation... qualifiers);

    /**
     * Never returns null. However, the handle is empty if no bean matches/multiple beans match the specified type and
     * qualifiers.
     *
     * @param type
     * @param qualifiers
     * @return a new instance handle
     */
    <T> InstanceHandle<T> instance(TypeLiteral<T> type, Annotation... qualifiers);

    /**
     * Never returns null. However, the handle is empty if no bean matches/multiple beans match the specified type and
     * qualifiers.
     *
     * @param type
     * @param qualifiers
     * @return a new instance handle
     */
    <X> InstanceHandle<X> instance(Type type, Annotation... qualifiers);

    /**
     * Never returns null. However, the handle is empty if no bean matches/multiple beans match the specified name.
     * 
     * @param name
     * @return a new instance handle
     * @see InjectableBean#getName()
     */
    <T> InstanceHandle<T> instance(String name);

    /**
     * Returns a supplier that can be used to create new instances, or null if no matching bean can be found.
     *
     * @param type
     * @param qualifiers
     * @param <T>
     * @return
     */
    <T> Supplier<InstanceHandle<T>> instanceSupplier(Class<T> type, Annotation... qualifiers);

    /**
     *
     * @param bean
     * @return a new bean instance handle
     */
    <T> InstanceHandle<T> instance(InjectableBean<T> bean);

    /**
     * Returns true if Arc container is running.
     * This can be used as a quick check to determine CDI availability in Quarkus.
     *
     * @return true is {@link ArcContainer} is running, false otherwise
     */
    boolean isRunning();

    /**
     *
     * @param beanIdentifier
     * @return an injectable bean or null
     * @see InjectableBean#getIdentifier()
     */
    <T> InjectableBean<T> bean(String beanIdentifier);

    /**
     * This method never throws {@link ContextNotActiveException}.
     * 
     * @return the built-in context for {@link javax.enterprise.context.RequestScoped}
     */
    ManagedContext requestContext();

    /**
     * NOTE: Not all methods are supported!
     *
     * @return the bean manager
     */
    BeanManager beanManager();

    /**
     * Indicates if there is a current request, and it has been turned asynchronous.
     * This uses the information provided by the currently registered
     * {@link AsyncRequestStatusProvider}.
     * 
     * @return true if there is a current request, and it has been turned asynchronous.
     */
    boolean isCurrentRequestAsync();

    /**
     * <p>
     * Returns a {@link CompletionStage} that represents completion of the current
     * asynchronous request. This uses the information provided by all currently registered
     * {@link AsyncRequestNotifierProvider}. The returned {@link CompletionStage} will notify
     * failure if any provider fails, or notify completion when all providers complete. This
     * allows us to get notified of exceptions in any part of the async request, even if other
     * parts are not aware of those failures.
     * </p>
     * <p>
     * If there is no current asynchronous request, this returns a completed {@link CompletionStage},
     * which will not reflect any eventual synchronous previously raised exception.
     * </p>
     * 
     * @return a {@link CompletionStage} to notify failure or completion of the current asynchronous request.
     */
    CompletionStage<Void> getAsyncRequestNotifier();
}
