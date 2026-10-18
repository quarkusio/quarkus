package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.DotName;

public class CustomAlterableContexts {
    private final List<CustomAlterableContextInfo> registered = new ArrayList<>();
    private final Predicate<DotName> applicationClassPredicate;

    CustomAlterableContexts(Predicate<DotName> applicationClassPredicate) {
        this.applicationClassPredicate = applicationClassPredicate;
    }

    public CustomAlterableContextInfo add(Class<? extends AlterableContext> contextClass, Boolean isNormal,
            Class<? extends Annotation> scopeAnnotation) {
        String generatedName = contextClass.getName() + "_InjectableContext";
        boolean isApplicationClass = applicationClassPredicate.test(DotName.createSimple(contextClass));
        CustomAlterableContextInfo result = new CustomAlterableContextInfo(contextClass, isNormal, generatedName,
                isApplicationClass, scopeAnnotation);
        registered.add(result);
        return result;
    }

    void validate(BeanDeploymentValidator.ValidationContext validationContext, boolean transformUnproxyableClasses,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        for (CustomAlterableContextInfo info : registered) {
            if (Modifier.isFinal(info.contextClass.getModifiers())) {
                if (transformUnproxyableClasses) {
                    bytecodeTransformerConsumer.accept(new BytecodeTransformer(info.contextClass.getName(),
                            new Beans.FinalClassTransformFunction()));
                } else {
                    validationContext.addDeploymentProblem(
                            new DeploymentException("Custom context class may not be final: " + info.contextClass));
                }
            }
        }
    }

    List<CustomAlterableContextInfo> getRegistered() {
        return registered;
    }

    public static class CustomAlterableContextInfo {
        public final Class<? extends AlterableContext> contextClass;
        public final Boolean isNormal;
        public final String generatedName;
        public final boolean isApplicationClass;
        public final Class<? extends Annotation> scopeAnnotation;

        CustomAlterableContextInfo(Class<? extends AlterableContext> contextClass, Boolean isNormal,
                String generatedName, boolean isApplicationClass, Class<? extends Annotation> scopeAnnotation) {
            this.contextClass = contextClass;
            this.isNormal = isNormal;
            this.generatedName = generatedName;
            this.isApplicationClass = isApplicationClass;
            this.scopeAnnotation = scopeAnnotation;
        }
    }
}
