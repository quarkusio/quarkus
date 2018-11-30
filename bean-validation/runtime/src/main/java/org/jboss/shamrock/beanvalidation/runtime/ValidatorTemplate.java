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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.validation.Validation;

import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.jboss.shamrock.runtime.Template;

@Template
public class ValidatorTemplate {

    public void initializeValidatorFactory(Set<Class<?>> classesToBeValidated) {
        PredefinedScopeHibernateValidatorConfiguration configuration = Validation.byProvider(PredefinedScopeHibernateValidator.class)
                .configure();

        Set<Locale> localesToInitialize = Collections.singleton(Locale.getDefault());

        try {
            Class<?> cl = Class.forName("javax.el.ELManager");
            Method method = cl.getDeclaredMethod("getExpressionFactory");
            method.invoke(null);
        } catch (Throwable t) {
            //if EL is not on the class path we use the parameter message interpolator
            configuration.messageInterpolator(new ParameterMessageInterpolator(localesToInitialize));
        }

        configuration
                .initializeBeanMetaData(classesToBeValidated)
                .initializeLocales(localesToInitialize);

        ValidatorHolder.initialize(configuration.buildValidatorFactory());
    }
}
