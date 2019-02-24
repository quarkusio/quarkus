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

package io.quarkus.arc.processor;

import org.jboss.jandex.DotName;
import io.quarkus.arc.InjectableBean;

/**
 * Allows a build-time extension to register synthetic {@link InjectableBean} implementations.
 *
 * @author Martin Kouba
 */
public interface BeanRegistrar extends BuildExtension {

    /**
     *
     * @param registrationContext
     */
    void register(RegistrationContext registrationContext);

    interface RegistrationContext extends BuildContext {

        /**
         *
         * @param beanClass
         * @return a new synthetic bean builder
         */
        <T> BeanConfigurator<T> configure(DotName beanClassName);

        default <T> BeanConfigurator<T> configure(Class<?> beanClass) {
            return configure(DotName.createSimple(beanClass.getName()));
        }

        // TODO add synthetic observer?

    }

}
