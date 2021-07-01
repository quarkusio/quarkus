package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.DefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

final class Decorators {

    static final Logger LOGGER = Logger.getLogger(Decorators.class);

    private Decorators() {
    }

    static DecoratorInfo createDecorator(ClassInfo decoratorClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer, AnnotationStore store) {

        // Find the delegate injection point
        List<InjectionPointInfo> delegateInjectionPoints = new LinkedList<>();
        List<Injection> injections = Injection.forBean(decoratorClass, null, beanDeployment, transformer);
        for (Injection injection : injections) {
            for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                if (injectionPoint.isDelegate()) {
                    delegateInjectionPoints.add(injectionPoint);
                }
            }
        }
        if (delegateInjectionPoints.isEmpty()) {
            throw new DefinitionException("The decorator " + decoratorClass + " has no @Delegate injection point");
        } else if (delegateInjectionPoints.size() > 1) {
            throw new DefinitionException(
                    "The decorator " + decoratorClass + " has multiple @Delegate injection points: " + delegateInjectionPoints);
        }

        InjectionPointInfo delegateInjectionPoint = delegateInjectionPoints.get(0);

        Integer priority = 0;
        boolean priorityDeclared = false;
        for (AnnotationInstance annotation : store.getAnnotations(decoratorClass)) {
            if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
                priorityDeclared = true;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
            if (scopeAnnotation != null && !BuiltinScope.DEPENDENT.is(scopeAnnotation)) {
                throw new DefinitionException(
                        "A decorator must be @Dependent but " + decoratorClass + " declares " + scopeAnnotation.getDotName());
            }
        }

        //  The set includes all bean types which are Java interfaces, except for java.io.Serializable
        Set<Type> types = Types.getClassBeanTypeClosure(decoratorClass, beanDeployment);
        Set<Type> decoratedTypes = new HashSet<>();
        for (Type type : types) {
            if (type.name().equals(DotNames.SERIALIZABLE)) {
                continue;
            }
            ClassInfo clazz = beanDeployment.getBeanArchiveIndex().getClassByName(type.name());
            if (Modifier.isInterface(clazz.flags())) {
                decoratedTypes.add(type);
            }
        }
        if (decoratedTypes.isEmpty()) {
            throw new DefinitionException("The decorator " + decoratorClass + " has no decorated type");
        }

        // The delegate type of a decorator must implement or extend every decorated type
        Set<Type> delegateTypes = Types.getDelegateTypeClosure(delegateInjectionPoint, beanDeployment);
        for (Type decoratedType : decoratedTypes) {
            if (!delegateTypes.contains(decoratedType)) {
                throw new DefinitionException("The delegate type " + delegateInjectionPoint.getRequiredType()
                        + " does not implement the decorated type: " + decoratedType);
            }
        }

        if (!priorityDeclared) {
            LOGGER.info("The decorator " + decoratorClass + " does not declare any @Priority. " +
                    "It will be assigned a default priority value of 0.");
        }

        if (Modifier.isAbstract(decoratorClass.flags())) {
            List<MethodInfo> abstractMethods = new ArrayList<>();
            for (MethodInfo method : decoratorClass.methods()) {
                if (Modifier.isAbstract(method.flags())) {
                    abstractMethods.add(method);
                }
            }
            if (!abstractMethods.isEmpty()) {
                throw new DefinitionException("An abstract decorator " + decoratorClass
                        + " declares abstract methods: " + abstractMethods);
            }
        }

        return new DecoratorInfo(decoratorClass, beanDeployment, delegateInjectionPoint,
                decoratedTypes, injections, priority);
    }

}
