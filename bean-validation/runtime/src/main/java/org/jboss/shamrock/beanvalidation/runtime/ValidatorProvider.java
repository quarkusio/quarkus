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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

@ApplicationScoped
public class ValidatorProvider {

    ValidatorFactory factory;

    @PostConstruct
    public void setup() {
        Configuration<?> configure = Validation.byDefaultProvider().configure();
        try {
            Class<?> cl = Class.forName("javax.el.ELManager");
            Method method = cl.getDeclaredMethod("getExpressionFactory");
            method.invoke(null);
        } catch (Throwable t) {
            //if EL is not on the class path we use the parameter message interpolator
            configure.messageInterpolator(new ParameterMessageInterpolator());
        }
        factory = configure.buildValidatorFactory();
    }

    @Produces
    public ValidatorFactory factory() {
        return factory;
    }

    @Produces
    public Validator validator() {
        return factory.getValidator();
    }

    public void forceInit() {

    }

}
