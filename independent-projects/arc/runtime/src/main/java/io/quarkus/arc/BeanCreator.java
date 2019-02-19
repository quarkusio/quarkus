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

package org.jboss.quarkus.arc;

import java.util.Map;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * It can be used by synthetic {@link InjectableBean} definitions to produce a contextual instance.
 *
 * @param <T>
 * @see Contextual#create(CreationalContext)
 */
public interface BeanCreator<T> {

    /**
     *
     * @param creationalContext
     * @param params
     * @return the contextual instance
     */
    T create(CreationalContext<T> creationalContext, Map<String, Object> params);

}