package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.Types.TypeClosure;

final class Decorators {

    static final Logger LOGGER = Logger.getLogger(Decorators.class);

    private Decorators() {
    }

    /**
     *
     * @param decoratorClass
     * @param beanDeployment
     * @param transformer
     * @return a new decorator info, or (only in strict mode) {@code null} if the decorator is disabled
     */
    static DecoratorInfo createDecorator(ClassInfo decoratorClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {

        // Find the delegate injection point
        List<InjectionPointInfo> delegateInjectionPoints = new ArrayList<>();
        List<Injection> injections = Injection.forBean(decoratorClass, null, beanDeployment, transformer,
                Injection.BeanType.DECORATOR);
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

        Integer priority = null;
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(decoratorClass)) {
            if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
            if (scopeAnnotation != null && !BuiltinScope.DEPENDENT.is(scopeAnnotation)) {
                throw new DefinitionException(
                        "A decorator must be @Dependent but " + decoratorClass + " declares " + scopeAnnotation.getDotName());
            }
        }

        //  The set includes all bean types which are Java interfaces, except for java.io.Serializable
        TypeClosure typeClosure = Types.getClassBeanTypeClosure(decoratorClass, beanDeployment);
        Set<Type> types = typeClosure.types();
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
            for (BuiltinBean bean : BuiltinBean.values()) {
                if (bean.equals(BuiltinBean.RESOURCE)) {
                    // do not take Resource into consideration
                    continue;
                }
                if (bean.hasRawTypeDotName(decoratedType.name())) {
                    throw new UnsupportedOperationException("Decorating built-in bean types is not supported! " +
                            "Decorator " + decoratorClass + " is attempting to decorate " + decoratedType.name());
                }
            }
        }

        if (priority == null) {
            if (beanDeployment.strictCompatibility) {
                // decorator without `@Priority` is disabled per the specification
                return null;
            }

            LOGGER.info("The decorator " + decoratorClass + " does not declare any @Priority. " +
                    "It will be assigned a default priority value of 0.");
            priority = 0;
        }

        if (Modifier.isAbstract(decoratorClass.flags())) {
            // Check all abstract methods in the hierarchy; reject those not belonging to a decorated type
            List<MethodInfo> invalidAbstractMethods = new ArrayList<>();
            ClassInfo currentClass = decoratorClass;
            IndexView index = beanDeployment.getBeanArchiveIndex();
            Set<DotName> visitedClasses = new HashSet<>();
            while (currentClass != null && !visitedClasses.contains(currentClass.name())) {
                visitedClasses.add(currentClass.name());
                for (MethodInfo method : currentClass.methods()) {
                    if (Modifier.isAbstract(method.flags())) {
                        boolean belongs = false;
                        Set<DotName> visitedTypes = new HashSet<>();
                        for (Type decoratedType : decoratedTypes) {
                            if (methodExistsInHierarchy(method, decoratedType, index, visitedTypes)) {
                                belongs = true;
                                break;
                            }
                        }
                        if (!belongs) {
                            invalidAbstractMethods.add(method);
                        }
                    }
                }
                DotName superClass = currentClass.superName();
                currentClass = superClass != null && !superClass.equals(DotNames.OBJECT)
                        ? getClassByName(index, superClass)
                        : null;
            }
            if (!invalidAbstractMethods.isEmpty()) {
                throw new DefinitionException("An abstract decorator " + decoratorClass
                        + " declares abstract methods that do not belong to a decorated type: " + invalidAbstractMethods);
            }
        }

        checkDecoratorFieldsAndMethods(decoratorClass, beanDeployment);

        return new DecoratorInfo(decoratorClass, beanDeployment, delegateInjectionPoint,
                decoratedTypes, injections, priority);
    }

    private static boolean methodExistsInHierarchy(MethodInfo method, Type type, IndexView index, Set<DotName> visited) {
        DotName typeName = type.name();
        if (visited.contains(typeName)) {
            return false;
        }
        visited.add(typeName);

        ClassInfo typeClass = index.getClassByName(typeName);
        if (typeClass == null) {
            return false;
        }

        // Check direct methods
        for (MethodInfo typeMethod : typeClass.methods()) {
            if (method.name().equals(typeMethod.name())
                    && method.parametersCount() == typeMethod.parametersCount()
                    && allParamsMatch(method, typeMethod)) {
                return true;
            }
        }

        // Recurse on superinterfaces
        for (Type superInterface : typeClass.interfaceTypes()) {
            if (methodExistsInHierarchy(method, superInterface, index, visited)) {
                return true;
            }
        }

        return false;
    }

    private static boolean allParamsMatch(MethodInfo m1, MethodInfo m2) {
        for (int cont = 0; cont < m1.parametersCount(); cont++) {
            if (!m1.parameterType(cont).name().equals(m2.parameterType(cont).name())) {
                return false;
            }
        }
        return true;
    }

    private static void checkDecoratorFieldsAndMethods(ClassInfo decoratorClass, BeanDeployment beanDeployment) {
        ClassInfo aClass = decoratorClass;
        Set<DotName> visited = new HashSet<>();
        while (aClass != null && !visited.contains(aClass.name())) {
            visited.add(aClass.name());
            for (MethodInfo method : aClass.methods()) {
                if (beanDeployment.hasAnnotation(method, DotNames.PRODUCES)) {
                    throw new DefinitionException("Decorator declares a producer method: " + decoratorClass);
                }
                // the following 3 checks rely on the annotation store returning parameter annotations for methods
                if (beanDeployment.hasAnnotation(method, DotNames.DISPOSES)) {
                    throw new DefinitionException("Decorator declares a disposer method: " + decoratorClass);
                }
                if (beanDeployment.hasAnnotation(method, DotNames.OBSERVES)) {
                    throw new DefinitionException("Decorator declares an observer method: " + decoratorClass);
                }
                if (beanDeployment.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                    throw new DefinitionException("Decorator declares an async observer method: " + decoratorClass);
                }
            }

            for (FieldInfo field : aClass.fields()) {
                if (beanDeployment.hasAnnotation(field, DotNames.PRODUCES)) {
                    throw new DefinitionException("Decorator declares a producer field: " + decoratorClass);
                }
            }

            DotName superClass = aClass.superName();
            aClass = superClass != null && !superClass.equals(DotNames.OBJECT)
                    ? getClassByName(beanDeployment.getBeanArchiveIndex(), superClass)
                    : null;
        }
    }

}