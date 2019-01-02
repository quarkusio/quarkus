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
import java.util.function.Supplier;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;

/**
 * TODO: consolidate {@link ArcContainer} and {@link InstanceHandle} API 
 * 
 * @author Martin Kouba
 */
public interface ArcContainer {

    /**
     *
     * @param scopeType
     * @return the context for the given scope, does not throw {@link javax.enterprise.context.ContextNotActiveException}
     */
    InjectableContext getContext(Class<? extends Annotation> scopeType);

    /**
     * Never returns null. However, the handle is empty if no bean matches/multiple beans match the specified type and qualifiers.
     *
     * @param type
     * @param qualifiers
     * @return a new instance handle
     */
    <T> InstanceHandle<T> instance(Class<T> type, Annotation... qualifiers);

    /**
     * Never returns null. However, the handle is empty if no bean matches/multiple beans match the specified type and qualifiers.
     *
     * @param type
     * @param qualifiers
     * @return a new instance handle
     */
    <T> InstanceHandle<T> instance(TypeLiteral<T> type, Annotation... qualifiers);

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
    *
    * @param beanIdentifier
    * @return an injectable bean or null
    * @see InjectableBean#getIdentifier()
    */
    <T> InjectableBean<T> bean(String beanIdentifier);
    
    /**
     *
     * @return the context for {@link javax.enterprise.context.RequestScoped}
     */
    ManagedContext requestContext();

    /**
     * Ensures the provided action will be performed with the request context active.
     *
     * Does not manage the context if it's already active.
     *
     * @param action
     */
    Runnable withinRequest(Runnable action);

    /**
     * Ensures the providedaction will be performed with the request context active.
     *
     * Does not manage the context if it's already active.
     *
     * @param action
     */
    <T> Supplier<T> withinRequest(Supplier<T> action);

    /**
     * NOTE: Not all methods are supported!
     *
     * @return the bean manager
     */
    BeanManager beanManager();

}
