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

import java.util.Collection;

import javax.enterprise.context.spi.AlterableContext;

/**
 *
 * @author Martin Kouba
 */
public interface InjectableContext extends AlterableContext {

    /**
     * Note that we cannot actually return just a map of contextuals to contextual instances because we need to preserve the
     * {@link javax.enterprise.context.spi.CreationalContext} too so that we're able to destroy the dependent objects correctly.
     *
     * @return all existing contextual instances
     */
    Collection<ContextInstanceHandle<?>> getAll();

    /**
     * Destroy all existing contextual instances.
     */
    void destroy();
}
