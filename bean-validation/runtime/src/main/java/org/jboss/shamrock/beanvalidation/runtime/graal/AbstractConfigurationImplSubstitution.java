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

package org.jboss.shamrock.beanvalidation.runtime.graal;

import java.util.function.Predicate;

import javax.validation.MessageInterpolator;

import org.hibernate.validator.internal.engine.AbstractConfigurationImpl;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

@TargetClass(AbstractConfigurationImpl.class)
final class AbstractConfigurationImplSubstitution {

    @Alias
    private MessageInterpolator defaultMessageInterpolator;

    @Substitute
    @TargetElement(onlyWith = ElPredicate.class)
    public final MessageInterpolator getDefaultMessageInterpolator() {
        if (defaultMessageInterpolator == null) {
            defaultMessageInterpolator = new ParameterMessageInterpolator();
        }

        return defaultMessageInterpolator;
    }

    @Substitute
    @TargetElement(onlyWith = ElPredicate.class)
    private MessageInterpolator getDefaultMessageInterpolatorConfiguredWithClassLoader() {
        return new ParameterMessageInterpolator();
    }


    static class ElPredicate implements Predicate<Class<?>> {

        @Override
        public boolean test(Class<?> o) {
            try {
                Class.forName("com.sun.el.ExpressionFactoryImpl");
                return false;
            } catch (Throwable t) {
            }
            return true;
        }
    }
}
