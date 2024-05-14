package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.NormalScope;

import org.jboss.jandex.DotName;

import io.quarkus.arc.InjectableContext;
import org.jboss.jandex.Type;

/**
 * Use this extension point to register a custom {@link InjectableContext} implementation.
 *
 * @author Martin Kouba
 */
public interface ContextRegistrar extends BuildExtension {

    /**
     *
     * @param registrationContext
     */
    void register(RegistrationContext registrationContext);

    interface RegistrationContext extends BuildContext {

        /**
         *
         * @param scopeAnnotation
         * @return a new custom context configurator
         */
        default ContextConfigurator configure(Class<? extends Annotation> scopeAnnotation) {
            return configure(DotName.createSimple(scopeAnnotation))
                    .normal(scopeAnnotation.isAnnotationPresent(NormalScope.class));
        }

        /**
         *
         * @param scopeAnnotation
         * @return a new custom context configurator
         */
        default ContextConfigurator configure(Type scopeAnnotation) {
            return configure(scopeAnnotation.name())
                    .normal(scopeAnnotation.hasAnnotation(DotNames.NORMAL_SCOPE));
        }

        /**
         *
         * @param scopeAnnotation
         * @return a new custom context configurator
         */
        ContextConfigurator configure(DotName scopeAnnotation);

    }

}
