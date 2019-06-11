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

import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;
import io.quarkus.arc.processor.InjectionTargetInfo.TargetKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.DefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

final class Beans {

    static final Logger LOGGER = Logger.getLogger(Beans.class);

    private Beans() {
    }

    /**
     *
     * @param beanClass
     * @param beanDeployment
     * @return a new bean info
     */
    static BeanInfo createClassBean(ClassInfo beanClass, BeanDeployment beanDeployment, InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getClassBeanTypeClosure(beanClass, Collections.emptyMap(), beanDeployment);
        Integer alternativePriority = null;
        boolean isAlternative = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(beanClass)) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                // Qualifiers
                qualifiers.add(annotation);
                if (DotNames.NAMED.equals(annotation.name())) {
                    AnnotationValue nameValue = annotation.value();
                    if (nameValue != null) {
                        name = nameValue.asString();
                    } else {
                        name = getDefaultName(beanClass);
                    }
                }
                continue;
            }
            if (annotation.name()
                    .equals(DotNames.ALTERNATIVE)) {
                isAlternative = true;
                continue;
            }
            if (annotation.name()
                    .equals(DotNames.PRIORITY)) {
                alternativePriority = annotation.value()
                        .asInt();
                continue;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
            if (scopeAnnotation != null) {
                scopes.add(scopeAnnotation);
                continue;
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotation.name());
            if (stereotype != null) {
                stereotypes.add(stereotype);
                continue;
            }
        }

        if (scopes.size() > 1) {
            throw multipleScopesFound("Bean class " + beanClass, scopes);
        }
        ScopeInfo scope;
        if (scopes.isEmpty()) {
            // try to search stereotypes for scope
            scope = initStereotypeScope(stereotypes, beanClass);
            // if that fails, try inheriting them
            if (scope == null) {
                scope = inheritScope(beanClass, beanDeployment);
            }
        } else {
            scope = scopes.get(0);
        }
        if (!isAlternative) {
            isAlternative = initStereotypeAlternative(stereotypes);
        }
        if (name == null) {
            name = initStereotypeName(stereotypes, beanClass);
        }

        BeanInfo bean = new BeanInfo(beanClass, beanDeployment, scope, types, qualifiers,
                Injection.forBean(beanClass, null, beanDeployment, transformer), null, null,
                isAlternative ? alternativePriority : null, stereotypes, name);
        return bean;
    }

    private static ScopeInfo inheritScope(ClassInfo beanClass, BeanDeployment beanDeployment) {
        DotName superClassName = beanClass.superName();
        while (!superClassName.equals(DotNames.OBJECT)) {
            ClassInfo classFromIndex = beanDeployment.getIndex().getClassByName(superClassName);
            if (classFromIndex == null) {
                // class not in index
                LOGGER.warnf("Unable to determine scope for bean %s using inheritance because its super class " +
                        "%s is not part of Jandex index. Dependent scope will be used instead.", beanClass, superClassName);
                return null;
            }
            for (AnnotationInstance annotation : beanDeployment.getAnnotationStore().getAnnotations(classFromIndex)) {
                ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
                if (scopeAnnotation != null && scopeAnnotation.declaresInherited()) {
                    // found some scope, return
                    return scopeAnnotation;
                }
            }
            superClassName = classFromIndex.superName();
        }
        // none found
        return null;
    }

    /**
     *
     * @param producerMethod
     * @param declaringBean
     * @param beanDeployment
     * @param disposer
     * @return a new bean info
     */
    static BeanInfo createProducerMethod(MethodInfo producerMethod, BeanInfo declaringBean, BeanDeployment beanDeployment,
            DisposerInfo disposer, InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getProducerMethodTypeClosure(producerMethod, beanDeployment);
        Integer alternativePriority = null;
        boolean isAlternative = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(producerMethod)) {
            //only check for method annotations since at this point we will get both
            // method and method param annotations
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
                if (DotNames.NAMED.equals(annotation.name())) {
                    AnnotationValue nameValue = annotation.value();
                    if (nameValue != null) {
                        name = nameValue.asString();
                    } else {
                        name = getDefaultName(producerMethod);
                    }
                }
                continue;
            }
            if (DotNames.ALTERNATIVE.equals(annotation.name())) {
                isAlternative = true;
                continue;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
            if (scopeAnnotation != null) {
                scopes.add(scopeAnnotation);
                continue;
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotation.name());
            if (stereotype != null) {
                stereotypes.add(stereotype);
                continue;
            }
        }

        if (scopes.size() > 1) {
            throw multipleScopesFound("Producer method " + producerMethod, scopes);
        }
        ScopeInfo scope;
        if (scopes.isEmpty()) {
            scope = initStereotypeScope(stereotypes, producerMethod);
        } else {
            scope = scopes.get(0);
        }
        if (!isAlternative) {
            isAlternative = initStereotypeAlternative(stereotypes);
        }
        if (name == null) {
            name = initStereotypeName(stereotypes, producerMethod);
        }
        if (isAlternative) {
            alternativePriority = declaringBean.getAlternativePriority();
            if (alternativePriority == null) {
                // Declaring bean itself does not have to be an alternative and can only have @Priority
                alternativePriority = declaringBean.getTarget().get().asClass().classAnnotations().stream()
                        .filter(a -> a.name().equals(DotNames.PRIORITY)).findAny()
                        .map(a -> a.value().asInt()).orElse(null);
            }
        }

        BeanInfo bean = new BeanInfo(producerMethod, beanDeployment, scope, types, qualifiers,
                Injection.forBean(producerMethod, declaringBean, beanDeployment, transformer), declaringBean,
                disposer, alternativePriority, stereotypes, name);
        return bean;
    }

    /**
     *
     * @param producerField
     * @param declaringBean
     * @param beanDeployment
     * @param disposer
     * @return a new bean info
     */
    static BeanInfo createProducerField(FieldInfo producerField, BeanInfo declaringBean, BeanDeployment beanDeployment,
            DisposerInfo disposer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getProducerFieldTypeClosure(producerField, beanDeployment);
        Integer alternativePriority = null;
        boolean isAlternative = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(producerField)) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
                if (DotNames.NAMED.equals(annotation.name())) {
                    AnnotationValue nameValue = annotation.value();
                    if (nameValue != null) {
                        name = nameValue.asString();
                    } else {
                        name = producerField.name();
                    }
                }
                continue;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
            if (scopeAnnotation != null) {
                scopes.add(scopeAnnotation);
                continue;
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotation.name());
            if (stereotype != null) {
                stereotypes.add(stereotype);
                continue;
            }
        }

        if (scopes.size() > 1) {
            throw multipleScopesFound("Producer field " + producerField, scopes);
        }
        ScopeInfo scope;
        if (scopes.isEmpty()) {
            scope = initStereotypeScope(stereotypes, producerField);
        } else {
            scope = scopes.get(0);
        }
        if (!isAlternative) {
            isAlternative = initStereotypeAlternative(stereotypes);
        }
        if (name == null) {
            name = initStereotypeName(stereotypes, producerField);
        }
        if (isAlternative) {
            alternativePriority = declaringBean.getAlternativePriority();
            if (alternativePriority == null) {
                // Declaring bean itself does not have to be an alternative and can only have @Priority
                alternativePriority = declaringBean.getTarget().get().asClass().classAnnotations().stream()
                        .filter(a -> a.name().equals(DotNames.PRIORITY)).findAny()
                        .map(a -> a.value().asInt()).orElse(null);
            }
        }

        BeanInfo bean = new BeanInfo(producerField, beanDeployment, scope, types, qualifiers, Collections.emptyList(),
                declaringBean, disposer,
                alternativePriority, stereotypes, name);
        return bean;
    }

    private static DefinitionException multipleScopesFound(String baseMessage, List<ScopeInfo> scopes) {
        return new DefinitionException(baseMessage + " declares multiple scope type annotations: "
                + scopes.stream().map(s -> s.getDotName().toString()).collect(Collectors.joining(", ")));
    }

    private static ScopeInfo initStereotypeScope(List<StereotypeInfo> stereotypes, AnnotationTarget target) {
        if (stereotypes.isEmpty()) {
            return null;
        }
        final Set<ScopeInfo> stereotypeScopes = new HashSet<>();
        for (StereotypeInfo stereotype : stereotypes) {
            stereotypeScopes.add(stereotype.getDefaultScope());
        }
        return BeanDeployment.getValidScope(stereotypeScopes, target);
    }

    private static boolean initStereotypeAlternative(List<StereotypeInfo> stereotypes) {
        if (stereotypes.isEmpty()) {
            return false;
        }
        for (StereotypeInfo stereotype : stereotypes) {
            if (stereotype.isAlternative()) {
                return true;
            }
        }
        return false;
    }

    private static String initStereotypeName(List<StereotypeInfo> stereotypes, AnnotationTarget target) {
        if (stereotypes.isEmpty()) {
            return null;
        }
        for (StereotypeInfo stereotype : stereotypes) {
            if (stereotype.isNamed()) {
                switch (target.kind()) {
                    case CLASS:
                        return getDefaultName(target.asClass());
                    case FIELD:
                        return target.asField()
                                .name();
                    case METHOD:
                        return getDefaultName(target.asMethod());
                    default:
                        break;
                }
            }
        }
        return null;
    }

    static boolean matches(BeanInfo bean, TypeAndQualifiers typeAndQualifiers) {
        // Bean has all the required qualifiers
        for (AnnotationInstance requiredQualifier : typeAndQualifiers.qualifiers) {
            if (!hasQualifier(bean, requiredQualifier)) {
                return false;
            }
        }
        // Bean has a bean type that matches the required type
        return matchesType(bean, typeAndQualifiers.type);
    }

    static boolean matchesType(BeanInfo bean, Type requiredType) {
        BeanResolver beanResolver = bean.getDeployment().getBeanResolver();
        for (Type beanType : bean.getTypes()) {
            if (beanResolver.matches(requiredType, beanType)) {
                return true;
            }
        }
        return false;
    }

    static void resolveInjectionPoint(BeanDeployment deployment, InjectionTargetInfo target, InjectionPointInfo injectionPoint,
            List<Throwable> errors) {
        BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
        if (builtinBean != null) {
            if (BuiltinBean.INJECTION_POINT.equals(builtinBean)
                    && (target.kind() != TargetKind.BEAN || !BuiltinScope.DEPENDENT.is(target.asBean().getScope()))) {
                errors.add(new DefinitionException("Only @Dependent beans can access metadata about an injection point: "
                        + injectionPoint.getTargetInfo()));
            }
            if (BuiltinBean.EVENT_METADATA.equals(builtinBean)
                    && target.kind() != TargetKind.OBSERVER) {
                errors.add(new DefinitionException("EventMetadata can be only injected into an observer method: "
                        + injectionPoint.getTargetInfo()));
            }
            // Skip built-in beans
            return;
        }
        List<BeanInfo> resolved = deployment.getBeanResolver().resolve(injectionPoint.getTypeAndQualifiers());
        BeanInfo selected = null;
        if (resolved.isEmpty()) {
            StringBuilder message = new StringBuilder("Unsatisfied dependency for type ");
            message.append(injectionPoint.getRequiredType());
            message.append(" and qualifiers ");
            message.append(injectionPoint.getRequiredQualifiers());
            message.append("\n\t- java member: ");
            message.append(injectionPoint.getTargetInfo());
            message.append("\n\t- declared on ");
            message.append(target);
            errors.add(new UnsatisfiedResolutionException(message.toString()));
        } else if (resolved.size() > 1) {
            // Try to resolve the ambiguity
            selected = resolveAmbiguity(resolved);
            if (selected == null) {
                StringBuilder message = new StringBuilder("Ambiguous dependencies for type ");
                message.append(injectionPoint.getRequiredType());
                message.append(" and qualifiers ");
                message.append(injectionPoint.getRequiredQualifiers());
                message.append("\n\t- java member: ");
                message.append(injectionPoint.getTargetInfo());
                message.append("\n\t- declared on ");
                message.append(target);
                message.append("\n\t- available beans:\n\t\t- ");
                message.append(resolved.stream().map(Object::toString).collect(Collectors.joining("\n\t\t- ")));
                errors.add(new AmbiguousResolutionException(message.toString()));
            }
        } else {
            selected = resolved.get(0);
        }
        if (selected != null) {
            injectionPoint.resolve(selected);
        }
    }

    static BeanInfo resolveAmbiguity(List<BeanInfo> resolved) {
        BeanInfo selected = null;
        List<BeanInfo> resolvedAmbiguity = new ArrayList<>(resolved);
        for (Iterator<BeanInfo> iterator = resolvedAmbiguity.iterator(); iterator.hasNext();) {
            BeanInfo beanInfo = iterator.next();
            if (!beanInfo.isAlternative() && (beanInfo.getDeclaringBean() == null || !beanInfo.getDeclaringBean()
                    .isAlternative())) {
                iterator.remove();
            }
        }
        if (resolvedAmbiguity.size() == 1) {
            selected = resolvedAmbiguity.get(0);
        } else if (resolvedAmbiguity.size() > 1) {
            // Keep only the highest priorities
            resolvedAmbiguity.sort(Beans::compareAlternativeBeans);
            Integer highest = getAlternativePriority(resolvedAmbiguity.get(0));
            for (Iterator<BeanInfo> iterator = resolvedAmbiguity.iterator(); iterator.hasNext();) {
                if (!highest.equals(getAlternativePriority(iterator.next()))) {
                    iterator.remove();
                }
            }
            if (resolved.size() == 1) {
                selected = resolvedAmbiguity.get(0);
            }
        }
        return selected;
    }

    private static Integer getAlternativePriority(BeanInfo bean) {
        return bean.getDeclaringBean() != null ? bean.getDeclaringBean().getAlternativePriority()
                : bean.getAlternativePriority();
    }

    private static int compareAlternativeBeans(BeanInfo bean1, BeanInfo bean2) {
        // The highest priority wins
        Integer priority2 = bean2.getDeclaringBean() != null ? bean2.getDeclaringBean().getAlternativePriority()
                : bean2.getAlternativePriority();
        Integer priority1 = bean1.getDeclaringBean() != null ? bean1.getDeclaringBean().getAlternativePriority()
                : bean1.getAlternativePriority();
        return priority2.compareTo(priority1);
    }

    static boolean hasQualifier(BeanInfo bean, AnnotationInstance required) {
        return hasQualifier(bean.getDeployment().getQualifier(required.name()), required, bean.getQualifiers());
    }

    static boolean hasQualifier(ClassInfo requiredInfo, AnnotationInstance required,
            Collection<AnnotationInstance> qualifiers) {
        List<AnnotationValue> binding = new ArrayList<>();
        for (AnnotationValue val : required.values()) {
            if (!requiredInfo.method(val.name()).hasAnnotation(DotNames.NONBINDING)) {
                binding.add(val);
            }
        }
        for (AnnotationInstance qualifier : qualifiers) {
            if (required.name().equals(qualifier.name())) {
                // Must have the same annotation member value for each member which is not annotated @Nonbinding
                boolean matches = true;
                for (AnnotationValue value : binding) {
                    if (!value.equals(qualifier.value(value.name()))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<MethodInfo> getCallbacks(ClassInfo beanClass, DotName annotation, IndexView index) {
        List<MethodInfo> callbacks = new ArrayList<>();
        collectCallbacks(beanClass, callbacks, annotation, index);
        Collections.reverse(callbacks);
        return callbacks;
    }

    static void analyzeType(Type type, BeanDeployment beanDeployment) {
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            for (Type argument : type.asParameterizedType().arguments()) {
                fetchType(argument, beanDeployment);
            }
        } else if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            for (Type bound : type.asTypeVariable().bounds()) {
                fetchType(bound, beanDeployment);
            }
        } else if (type.kind() == Type.Kind.WILDCARD_TYPE) {
            fetchType(type.asWildcardType().extendsBound(), beanDeployment);
            fetchType(type.asWildcardType().superBound(), beanDeployment);
        }
    }

    private static void fetchType(Type type, BeanDeployment beanDeployment) {
        if (type == null) {
            return;
        }
        if (type.kind() == Type.Kind.CLASS) {
            // Index the class additionally if needed
            beanDeployment.getIndex().getClassByName(type.name());
        } else {
            analyzeType(type, beanDeployment);
        }
    }

    private static void collectCallbacks(ClassInfo clazz, List<MethodInfo> callbacks, DotName annotation, IndexView index) {
        for (MethodInfo method : clazz.methods()) {
            if (method.hasAnnotation(annotation) && method.returnType().kind() == Kind.VOID && method.parameters().isEmpty()) {
                callbacks.add(method);
            }
        }
        if (clazz.superName() != null) {
            ClassInfo superClass = index.getClassByName(clazz.superName());
            if (superClass != null) {
                collectCallbacks(superClass, callbacks, annotation, index);
            }
        }
    }

    private static String getPropertyName(String methodName) {
        final String get = "get";
        final String is = "is";
        if (methodName.startsWith(get)) {
            return decapitalize(methodName.substring(get.length()));
        } else if (methodName.startsWith(is)) {
            return decapitalize(methodName.substring(is.length()));
        } else {
            // The method is not a JavaBean property
            return null;
        }

    }

    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            // "URL" stays "URL"
            return name;
        }
        StringBuilder decapitalized = new StringBuilder(name);
        decapitalized.setCharAt(0, Character.toLowerCase(decapitalized.charAt(0)));
        return decapitalized.toString();
    }

    private static String getDefaultName(ClassInfo beanClass) {
        StringBuilder defaultName = new StringBuilder();
        defaultName.append(DotNames.simpleName(beanClass));
        // URLMatcher becomes uRLMatcher
        defaultName.setCharAt(0, Character.toLowerCase(defaultName.charAt(0)));
        return defaultName.toString();
    }

    private static String getDefaultName(MethodInfo producerMethod) {
        String propertyName = getPropertyName(producerMethod.name());
        if (propertyName != null) {
            // getURLMatcher() becomes URLMatcher
            return propertyName;
        } else {
            return producerMethod.name();
        }
    }

}
