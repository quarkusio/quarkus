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

package org.jboss.shamrock.beanvalidation.runtime;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.InjectionInstance;

@Template
public class ValidatorTemplate {

    /**
     * Force the validation factory to be created at static init time, so it is
     * bootstrapped in a JVM rather than in native-image
     * <p>
     * TODO: we really only need to run in native-image
     *
     * @param provider
     */
    public void forceInit(InjectionFactory provider, Class<?>... classesToValidate) {
        ValidatorProvider validatorProvider = provider.create(ValidatorProvider.class).newInstance();
        validatorProvider.forceInit();
        Validator validator = validatorProvider.factory().getValidator();
        for (Class<?> i : classesToValidate) {
            validator.getConstraintsForClass(i);
        }
    }
}
