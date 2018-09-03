package org.jboss.shamrock.beanvalidation.runtime.graal;

import java.util.function.Predicate;

import javax.validation.MessageInterpolator;

import org.hibernate.validator.internal.engine.ConfigurationImpl;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

@TargetClass(ConfigurationImpl.class)
final class ConfigurationImplSubstitution {

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
