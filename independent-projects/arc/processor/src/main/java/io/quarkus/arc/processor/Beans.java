package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;

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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;
import io.quarkus.gizmo.Gizmo;

public final class Beans {

    static final Logger LOGGER = Logger.getLogger(Beans.class);

    private Beans() {
    }

    static BeanInfo createClassBean(ClassInfo beanClass, BeanDeployment beanDeployment, InjectionPointModifier transformer) {
        return new ClassBeanFactory(beanClass, beanDeployment, transformer).create();
    }

    private static ScopeInfo inheritScope(ClassInfo beanClass, BeanDeployment beanDeployment) {
        DotName superClassName = beanClass.superName();
        while (!superClassName.equals(DotNames.OBJECT)) {
            ClassInfo classFromIndex = getClassByName(beanDeployment.getBeanArchiveIndex(), superClassName);
            if (classFromIndex == null) {
                // class not in index
                LOGGER.warnf("Unable to determine scope for bean %s using inheritance because its super class " +
                        "%s is not part of Jandex index. Dependent scope will be used instead.", beanClass, superClassName);
                return null;
            }
            for (AnnotationInstance annotation : beanDeployment.getAnnotationStore().getAnnotations(classFromIndex)) {
                ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
                if (scopeAnnotation != null) {
                    // found some scope, return it if it's inherited
                    // if it isn't inherited, it still prevents the bean class
                    // from inheriting another scope from a further superclass
                    return scopeAnnotation.declaresInherited() ? scopeAnnotation : null;
                }
            }
            superClassName = classFromIndex.superName();
        }
        // none found
        return null;
    }

    static BeanInfo createProducerMethod(Set<Type> beanTypes, MethodInfo producerMethod, BeanInfo declaringBean,
            BeanDeployment beanDeployment,
            DisposerInfo disposer, InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Integer priority = null;
        boolean isAlternative = false;
        boolean isDefaultBean = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        Set<ScopeInfo> beanDefiningAnnotationScopes = new HashSet<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(producerMethod)) {
            DotName annotationName = annotation.name();
            //only check for method annotations since at this point we will get both
            // method and method param annotations
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (DotNames.NAMED.equals(annotationName)) {
                AnnotationValue nameValue = annotation.value();
                if (nameValue != null) {
                    name = nameValue.asString();
                } else {
                    name = getDefaultName(producerMethod);
                    annotation = normalizedNamedQualifier(name, annotation);
                }
            }
            BeanDefiningAnnotation bda = beanDeployment.getBeanDefiningAnnotation(annotationName);
            if (bda != null && bda.getDefaultScope() != null) {
                beanDefiningAnnotationScopes.add(beanDeployment.getScope(bda.getDefaultScope()));
            }
            Collection<AnnotationInstance> qualifierCollection = beanDeployment.extractQualifiers(annotation);
            for (AnnotationInstance qualifierAnnotation : qualifierCollection) {
                // Qualifiers
                qualifiers.add(qualifierAnnotation);
            }
            if (!qualifierCollection.isEmpty()) {
                // we needn't process it further, the annotation was a qualifier (or multiple repeating ones)
                continue;
            }
            if (DotNames.ALTERNATIVE.equals(annotationName)) {
                isAlternative = true;
                continue;
            }
            if (DotNames.PRIORITY.equals(annotationName)) {
                priority = annotation.value().asInt();
                continue;
            }
            if (priority == null && DotNames.ARC_PRIORITY.equals(annotationName)) {
                priority = annotation.value().asInt();
                continue;
            }
            if (DotNames.DEFAULT_BEAN.equals(annotationName)) {
                isDefaultBean = true;
                continue;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotationName);
            if (scopeAnnotation != null) {
                scopes.add(scopeAnnotation);
                continue;
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotationName);
            if (stereotype != null) {
                stereotypes.add(stereotype);
                continue;
            }
        }

        if (scopes.size() > 1) {
            throw multipleScopesFound("Producer method " + producerMethod, scopes);
        }
        // 1. Explicit scope
        // 2. Stereotype scope
        // 3. Bean defining annotation default scope
        ScopeInfo scope;
        if (scopes.isEmpty()) {
            scope = initStereotypeScope(stereotypes, producerMethod, beanDeployment);
            if (scope == null) {
                scope = initBeanDefiningAnnotationScope(beanDefiningAnnotationScopes, producerMethod);
            }
        } else {
            scope = scopes.get(0);
        }

        if (!isAlternative) {
            isAlternative = initStereotypeAlternative(stereotypes, beanDeployment);
        }
        if (name == null) {
            name = initStereotypeName(stereotypes, producerMethod, beanDeployment);
        }

        if (isAlternative) {
            if (priority == null) {
                priority = declaringBean.getPriority();
            }
            priority = initAlternativePriority(producerMethod, priority, stereotypes, beanDeployment);
            if (priority == null) {
                // after all attempts, priority is still null, bean will be ignored
                LOGGER.debugf(
                        "Ignoring producer method %s - declared as an @Alternative but not selected by @Priority or quarkus.arc.selected-alternatives",
                        declaringBean.getTarget().get().asClass().name() + "#" + producerMethod.name());
                return null;
            }
        }

        if (scope != null // `null` is just like `@Dependent`
                && !BuiltinScope.DEPENDENT.is(scope)
                && producerMethod.returnType().kind() == Kind.PARAMETERIZED_TYPE
                && Types.containsTypeVariable(producerMethod.returnType())) {
            throw new DefinitionException("Producer method return type is a parameterized type with a type variable, "
                    + "its scope must be @Dependent: " + producerMethod);
        }

        List<Injection> injections = Injection.forBean(producerMethod, declaringBean, beanDeployment, transformer,
                Injection.BeanType.PRODUCER_METHOD);
        BeanInfo bean = new BeanInfo(producerMethod, beanDeployment, scope, beanTypes, qualifiers, injections, declaringBean,
                disposer, isAlternative, stereotypes, name, isDefaultBean, null, priority);
        for (Injection injection : injections) {
            injection.init(bean);
        }
        return bean;
    }

    static BeanInfo createProducerField(FieldInfo producerField, BeanInfo declaringBean, BeanDeployment beanDeployment,
            DisposerInfo disposer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getProducerFieldTypeClosure(producerField, beanDeployment);
        Integer priority = null;
        boolean isAlternative = false;
        boolean isDefaultBean = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        Set<ScopeInfo> beanDefiningAnnotationScopes = new HashSet<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(producerField)) {
            DotName annotationName = annotation.name();
            if (DotNames.NAMED.equals(annotationName)) {
                AnnotationValue nameValue = annotation.value();
                if (nameValue != null) {
                    name = nameValue.asString();
                } else {
                    name = producerField.name();
                    annotation = normalizedNamedQualifier(name, annotation);
                }
            }
            BeanDefiningAnnotation bda = beanDeployment.getBeanDefiningAnnotation(annotationName);
            if (bda != null && bda.getDefaultScope() != null) {
                beanDefiningAnnotationScopes.add(beanDeployment.getScope(bda.getDefaultScope()));
            }
            Collection<AnnotationInstance> qualifierCollection = beanDeployment.extractQualifiers(annotation);
            for (AnnotationInstance qualifierAnnotation : qualifierCollection) {
                // Qualifiers
                qualifiers.add(qualifierAnnotation);
            }
            if (!qualifierCollection.isEmpty()) {
                // we needn't process it further, the annotation was a qualifier (or multiple repeating ones)
                continue;
            }
            if (DotNames.ALTERNATIVE.equals(annotationName)) {
                isAlternative = true;
                continue;
            }
            if (DotNames.PRIORITY.equals(annotation.name())) {
                priority = annotation.value().asInt();
                continue;
            }
            if (priority == null && DotNames.ARC_PRIORITY.equals(annotationName)) {
                priority = annotation.value().asInt();
                continue;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotationName);
            if (scopeAnnotation != null) {
                scopes.add(scopeAnnotation);
                continue;
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotationName);
            if (stereotype != null) {
                stereotypes.add(stereotype);
                continue;
            }
            if (DotNames.DEFAULT_BEAN.equals(annotationName)) {
                isDefaultBean = true;
                continue;
            }
        }

        if (scopes.size() > 1) {
            throw multipleScopesFound("Producer field " + producerField, scopes);
        }
        // 1. Explicit scope
        // 2. Stereotype scope
        // 3. Bean defining annotation default scope
        ScopeInfo scope;
        if (scopes.isEmpty()) {
            scope = initStereotypeScope(stereotypes, producerField, beanDeployment);
            if (scope == null) {
                scope = initBeanDefiningAnnotationScope(beanDefiningAnnotationScopes, producerField);
            }
        } else {
            scope = scopes.get(0);
        }
        if (!isAlternative) {
            isAlternative = initStereotypeAlternative(stereotypes, beanDeployment);
        }
        if (name == null) {
            name = initStereotypeName(stereotypes, producerField, beanDeployment);
        }

        if (isAlternative) {
            if (priority == null) {
                priority = declaringBean.getPriority();
            }
            priority = initAlternativePriority(producerField, priority, stereotypes, beanDeployment);
            // after all attempts, priority is still null
            if (priority == null) {
                LOGGER.debugf(
                        "Ignoring producer field %s - declared as an @Alternative but not selected by @Priority or quarkus.arc.selected-alternatives",
                        producerField);
                return null;
            }
        }

        if (scope != null // `null` is just like `@Dependent`
                && !BuiltinScope.DEPENDENT.is(scope)
                && producerField.type().kind() == Kind.PARAMETERIZED_TYPE
                && Types.containsTypeVariable(producerField.type())) {
            throw new DefinitionException("Producer field type is a parameterized type with a type variable, "
                    + "its scope must be @Dependent: " + producerField);
        }

        BeanInfo bean = new BeanInfo(producerField, beanDeployment, scope, types, qualifiers, Collections.emptyList(),
                declaringBean, disposer, isAlternative, stereotypes, name, isDefaultBean, null, priority);
        return bean;
    }

    private static AnnotationInstance normalizedNamedQualifier(String defaultedName, AnnotationInstance originalAnnotation) {
        // Replace @Named("") with @Named("foo")
        // This is not explicitly defined by the spec but better align with the RI behavior
        return AnnotationInstance.create(DotNames.NAMED, originalAnnotation.target(),
                Collections.singletonList(AnnotationValue.createStringValue("value", defaultedName)));
    }

    private static DefinitionException multipleScopesFound(String baseMessage, List<ScopeInfo> scopes) {
        return new DefinitionException(baseMessage + " declares multiple scope type annotations: "
                + scopes.stream().map(s -> s.getDotName().toString()).collect(Collectors.joining(", ")));
    }

    static ScopeInfo initStereotypeScope(List<StereotypeInfo> stereotypes, AnnotationTarget target,
            BeanDeployment beanDeployment) {
        if (stereotypes.isEmpty()) {
            return null;
        }

        Set<ScopeInfo> stereotypeScopes = new HashSet<>();
        for (StereotypeInfo stereotype : stereotypesWithTransitive(stereotypes, beanDeployment.getStereotypesMap())) {
            if (stereotype.getDefaultScope() != null) {
                stereotypeScopes.add(stereotype.getDefaultScope());
            }
        }

        return BeanDeployment.getValidScope(stereotypeScopes, target);
    }

    static ScopeInfo initBeanDefiningAnnotationScope(Set<ScopeInfo> beanDefiningAnnotationScopes, AnnotationTarget target) {
        if (beanDefiningAnnotationScopes.isEmpty()) {
            return null;
        }
        return BeanDeployment.getValidScope(beanDefiningAnnotationScopes, target);
    }

    static boolean initStereotypeAlternative(List<StereotypeInfo> stereotypes, BeanDeployment beanDeployment) {
        if (stereotypes.isEmpty()) {
            return false;
        }

        for (StereotypeInfo stereotype : stereotypesWithTransitive(stereotypes, beanDeployment.getStereotypesMap())) {
            if (stereotype.isAlternative()) {
                return true;
            }
        }

        return false;
    }

    // called when we know the bean does not declare priority on its own
    // therefore, we can just throw when multiple priorities are inherited from stereotypes
    static Integer initStereotypeAlternativePriority(List<StereotypeInfo> stereotypes, AnnotationTarget target,
            BeanDeployment beanDeployment) {
        if (stereotypes.isEmpty()) {
            return null;
        }

        Set<Integer> priorities = new HashSet<>();
        for (StereotypeInfo stereotype : stereotypesWithTransitive(stereotypes, beanDeployment.getStereotypesMap())) {
            if (stereotype.getAlternativePriority() != null) {
                priorities.add(stereotype.getAlternativePriority());
            }
        }

        if (priorities.isEmpty()) {
            return null;
        } else if (priorities.size() == 1) {
            return priorities.iterator().next();
        } else {
            throw new DefinitionException("Bean " + target
                    + " does not declare @Priority and inherits multiple different priorities from stereotypes");
        }
    }

    static String initStereotypeName(List<StereotypeInfo> stereotypes, AnnotationTarget target,
            BeanDeployment beanDeployment) {
        if (stereotypes.isEmpty()) {
            return null;
        }

        for (StereotypeInfo stereotype : stereotypesWithTransitive(stereotypes, beanDeployment.getStereotypesMap())) {
            if (stereotype.isNamed()) {
                switch (target.kind()) {
                    case CLASS:
                        return getDefaultName(target.asClass());
                    case FIELD:
                        return target.asField().name();
                    case METHOD:
                        return getDefaultName(target.asMethod());
                    default:
                        break;
                }
            }
        }

        return null;
    }

    public static List<StereotypeInfo> stereotypesWithTransitive(List<StereotypeInfo> stereotypes,
            Map<DotName, StereotypeInfo> allStereotypes) {
        List<StereotypeInfo> result = new ArrayList<>();
        Set<DotName> alreadySeen = new HashSet<>(); // to guard against hypothetical stereotype cycle
        Deque<StereotypeInfo> workQueue = new ArrayDeque<>(stereotypes);
        while (!workQueue.isEmpty()) {
            StereotypeInfo stereotype = workQueue.poll();
            if (alreadySeen.contains(stereotype.getName())) {
                continue;
            }
            result.add(stereotype);
            alreadySeen.add(stereotype.getName());

            for (AnnotationInstance parentStereotype : stereotype.getParentStereotypes()) {
                StereotypeInfo parent = allStereotypes.get(parentStereotype.name());
                if (parent != null) {
                    workQueue.add(parent);
                }
            }
        }

        return result;
    }

    /**
     * Checks if given {@link BeanInfo} has type and qualifiers matching those in provided {@link TypeAndQualifiers}.
     * Uses standard bean assignability rules; see {@link BeanResolverImpl}.
     */
    public static boolean matches(BeanInfo bean, TypeAndQualifiers typeAndQualifiers) {
        return bean.getDeployment().getBeanResolver().matches(bean, typeAndQualifiers);
    }

    /**
     * Checks if given {@link BeanInfo} has all the required qualifiers and a bean type that matches required type.
     * Uses standard bean assignability rules; see {@link BeanResolverImpl}.
     */
    static boolean matches(BeanInfo bean, Type requiredType, Set<AnnotationInstance> requiredQualifiers) {
        return bean.getDeployment().getBeanResolver().matches(bean, requiredType, requiredQualifiers);
    }

    static void resolveInjectionPoint(BeanDeployment deployment, InjectionTargetInfo target, InjectionPointInfo injectionPoint,
            List<Throwable> errors) {
        if (injectionPoint.isDelegate()) {
            // Skip delegate injection points
            return;
        }
        BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
        if (builtinBean != null) {
            builtinBean.validate(target, injectionPoint, errors::add);
            // Skip built-in beans
            return;
        }
        List<BeanInfo> resolved = deployment.beanResolver.resolve(injectionPoint.getTypeAndQualifiers());
        BeanInfo selected = null;
        if (resolved.isEmpty()) {
            List<BeanInfo> typeMatching = deployment.beanResolver.findTypeMatching(injectionPoint.getRequiredType());

            StringBuilder message = new StringBuilder("Unsatisfied dependency for type ");
            addStandardErroneousDependencyMessage(target, injectionPoint, message);
            if (!typeMatching.isEmpty()) {
                message.append("\n\tThe following beans match by type, but none have matching qualifiers:");
                for (BeanInfo beanInfo : typeMatching) {
                    message.append("\n\t\t- ");
                    message.append("Bean [class=");
                    message.append(beanInfo.getImplClazz());
                    message.append(", qualifiers=");
                    message.append(beanInfo.getQualifiers());
                    message.append("]");
                }
            }
            errors.add(new UnsatisfiedResolutionException(message.toString()));
        } else if (resolved.size() > 1) {
            // Try to resolve the ambiguity
            selected = resolveAmbiguity(resolved);
            if (selected == null) {
                StringBuilder message = new StringBuilder("Ambiguous dependencies for type ");
                addStandardErroneousDependencyMessage(target, injectionPoint, message);
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

    private static void addStandardErroneousDependencyMessage(InjectionTargetInfo target, InjectionPointInfo injectionPoint,
            StringBuilder message) {
        message.append(injectionPoint.getType());
        message.append(" and qualifiers ");
        message.append(injectionPoint.getRequiredQualifiers());
        if (injectionPoint.isSynthetic()) {
            message.append("\n\t- synthetic injection point");
        } else {
            message.append("\n\t- java member: ");
            message.append(injectionPoint.getTargetInfo());
        }
        message.append("\n\t- declared on ");
        message.append(target);
    }

    static BeanInfo resolveAmbiguity(Collection<BeanInfo> resolved) {
        List<BeanInfo> resolvedAmbiguity = new ArrayList<>(resolved);
        // First eliminate default beans
        for (Iterator<BeanInfo> iterator = resolvedAmbiguity.iterator(); iterator.hasNext();) {
            BeanInfo beanInfo = iterator.next();
            if (beanInfo.isDefaultBean()) {
                iterator.remove();
            }
        }
        if (resolvedAmbiguity.size() == 1) {
            return resolvedAmbiguity.get(0);
        }

        BeanInfo selected = null;
        for (Iterator<BeanInfo> iterator = resolvedAmbiguity.iterator(); iterator.hasNext();) {
            BeanInfo beanInfo = iterator.next();
            // Eliminate beans that are not alternatives, except for producer methods and fields of beans that are alternatives
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
            if (resolvedAmbiguity.size() == 1) {
                selected = resolvedAmbiguity.get(0);
            }
        }
        return selected;
    }

    private static Integer getAlternativePriority(BeanInfo bean) {
        Integer beanPriority = bean.getAlternativePriority();
        if (beanPriority == null && bean.getDeclaringBean() != null) {
            beanPriority = bean.getDeclaringBean().getAlternativePriority();
        }
        return beanPriority;
    }

    private static int compareAlternativeBeans(BeanInfo bean1, BeanInfo bean2) {
        // The highest priority wins
        Integer priority1, priority2;

        priority2 = bean2.getAlternativePriority();
        if (priority2 == null) {
            priority2 = bean2.getDeclaringBean().getAlternativePriority();
        }

        priority1 = bean1.getAlternativePriority();
        if (priority1 == null) {
            priority1 = bean1.getDeclaringBean().getAlternativePriority();
        }

        if (priority2 == null || priority1 == null) {
            throw new IllegalStateException(String.format(
                    "Alternative Bean priority should not be null. %s has priority %s; %s has priority %s",
                    bean1.getBeanClass(), priority1,
                    bean2.getBeanClass(), priority2));
        }

        return priority2.compareTo(priority1);
    }

    static boolean hasQualifiers(BeanInfo bean, Iterable<AnnotationInstance> required) {
        for (AnnotationInstance requiredQualifier : required) {
            if (!hasQualifier(bean, requiredQualifier)) {
                return false;
            }
        }
        return true;
    }

    static boolean hasQualifier(BeanInfo bean, AnnotationInstance required) {
        return hasQualifier(bean.getDeployment(), required, bean.getQualifiers());
    }

    static boolean hasQualifier(BeanDeployment beanDeployment, AnnotationInstance requiredQualifier,
            Collection<AnnotationInstance> qualifiers) {
        ClassInfo requiredClazz = beanDeployment.getQualifier(requiredQualifier.name());
        List<AnnotationValue> values = null;
        for (AnnotationInstance qualifier : qualifiers) {
            if (requiredQualifier.name().equals(qualifier.name())) {
                // Must have the same annotation member value for each member which is not annotated @Nonbinding
                boolean matches = true;

                if (values == null) {
                    //this list is relatively expensive to initialize in some cases
                    //as this is called in a tight loop we only do it if necessary
                    values = new ArrayList<>();
                    Set<String> nonBindingFields = beanDeployment.getQualifierNonbindingMembers(requiredQualifier.name());
                    for (AnnotationValue val : requiredQualifier.valuesWithDefaults(beanDeployment.getBeanArchiveIndex())) {
                        if (!requiredClazz.method(val.name()).hasAnnotation(DotNames.NONBINDING)
                                && !nonBindingFields.contains(val.name())) {
                            values.add(val);
                        }
                    }
                }
                for (AnnotationValue value : values) {
                    if (!value.equals(qualifier.valueWithDefault(beanDeployment.getBeanArchiveIndex(), value.name()))) {
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

    static void addImplicitQualifiers(Set<AnnotationInstance> qualifiers) {
        if (qualifiers.isEmpty()
                || (qualifiers.size() <= 2 && qualifiers.stream()
                        .allMatch(a -> DotNames.NAMED.equals(a.name()) || DotNames.ANY.equals(a.name())))) {
            qualifiers.add(BuiltinQualifier.DEFAULT.getInstance());
        }
        qualifiers.add(BuiltinQualifier.ANY.getInstance());
    }

    static List<MethodInfo> getCallbacks(ClassInfo beanClass, DotName annotation, IndexView index) {
        List<MethodInfo> callbacks = new ArrayList<>();
        collectCallbacks(beanClass, callbacks, annotation, index, new HashSet<>());
        Collections.reverse(callbacks);
        return callbacks;
    }

    static List<MethodInfo> getAroundInvokes(ClassInfo beanClass, BeanDeployment deployment) {
        AnnotationStore store = deployment.getAnnotationStore();
        List<MethodInfo> methods = new ArrayList<>();

        List<MethodInfo> allMethods = new ArrayList<>();
        ClassInfo aClass = beanClass;
        while (aClass != null) {
            int aroundInvokesFound = 0;
            for (MethodInfo method : aClass.methods()) {
                if (Modifier.isStatic(method.flags())) {
                    continue;
                }
                if (store.hasAnnotation(method, DotNames.AROUND_INVOKE)) {
                    InterceptorInfo.addInterceptorMethod(allMethods, methods, method);
                    if (++aroundInvokesFound > 1) {
                        throw new DefinitionException(
                                "Multiple @AroundInvoke interceptor methods declared on class: " + aClass);
                    }
                }
                allMethods.add(method);
            }
            DotName superTypeName = aClass.superName();
            aClass = superTypeName == null || DotNames.OBJECT.equals(superTypeName) ? null
                    : getClassByName(deployment.getBeanArchiveIndex(), superTypeName);
        }
        Collections.reverse(methods);
        return methods.isEmpty() ? List.of() : List.copyOf(methods);
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

    static void validateInterceptorDecorator(BeanInfo bean, List<Throwable> errors,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        // transform any private injected fields into package private
        if (bean.isClassBean() && bean.getDeployment().transformPrivateInjectedFields) {
            for (Injection injection : bean.getInjections()) {
                if (injection.isField() && Modifier.isPrivate(injection.getTarget().asField().flags())) {
                    bytecodeTransformerConsumer
                            .accept(new BytecodeTransformer(bean.getTarget().get().asClass().name().toString(),
                                    new PrivateInjectedFieldTransformFunction(injection.getTarget().asField().name())));
                }
            }
        }
    }

    static void validateBean(BeanInfo bean, List<Throwable> errors, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            Set<DotName> classesReceivingNoArgsCtor) {
        if (bean.isClassBean()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            String classifier = bean.getScope().isNormal() ? "Normal scoped" : null;
            if (classifier == null && bean.isSubclassRequired()) {
                classifier = "Intercepted";
            }
            if (Modifier.isFinal(beanClass.flags()) && classifier != null) {
                // Client proxies and subclasses require a non-final class
                if (bean.getDeployment().transformUnproxyableClasses) {
                    bytecodeTransformerConsumer
                            .accept(new BytecodeTransformer(beanClass.name().toString(), new FinalClassTransformFunction()));
                } else {
                    errors.add(new DeploymentException(String.format("%s bean must not be final: %s", classifier, bean)));
                }
            }
            if (bean.getDeployment().strictCompatibility && classifier != null) {
                validateNonStaticFinalMethods(beanClass, bean.getDeployment().getBeanArchiveIndex(),
                        classifier, errors);
            }

            MethodInfo noArgsConstructor = beanClass.method(Methods.INIT);
            // Note that spec also requires no-arg constructor for intercepted beans but intercepted subclasses should work fine with non-private @Inject
            // constructors so we only validate normal scoped beans
            if (bean.getScope().isNormal() && noArgsConstructor == null) {
                if (bean.getDeployment().transformUnproxyableClasses) {
                    DotName superName = beanClass.superName();
                    if (!DotNames.OBJECT.equals(superName)) {
                        ClassInfo superClass = bean.getDeployment().getBeanArchiveIndex().getClassByName(beanClass.superName());
                        if (superClass == null || !superClass.hasNoArgsConstructor()) {
                            // Bean class extends a class without no-args constructor
                            // It is not possible to generate a no-args constructor reliably
                            superName = null;
                        }
                    }
                    if (superName != null) {
                        if (!classesReceivingNoArgsCtor.contains(beanClass.name())) {
                            String superClassName = superName.toString().replace('.', '/');
                            bytecodeTransformerConsumer.accept(new BytecodeTransformer(beanClass.name().toString(),
                                    new NoArgConstructorTransformFunction(superClassName)));
                            classesReceivingNoArgsCtor.add(beanClass.name());
                        }

                    } else {
                        errors.add(cannotAddSyntheticNoArgsConstructor(beanClass));
                    }
                } else {
                    errors.add(new DeploymentException(String
                            .format("Normal scoped beans must declare a non-private constructor with no parameters: %s",
                                    bean)));
                }
            }

            if (noArgsConstructor != null && Modifier.isPrivate(noArgsConstructor.flags()) && classifier != null) {
                // Client proxies and subclasses require a non-private no-args constructor
                if (bean.getDeployment().transformUnproxyableClasses) {
                    bytecodeTransformerConsumer.accept(
                            new BytecodeTransformer(beanClass.name().toString(),
                                    new PrivateNoArgsConstructorTransformFunction()));
                } else {
                    errors.add(
                            new DeploymentException(
                                    String.format(
                                            "%s bean is not proxyable because it has a private no-args constructor: %s. To fix this problem, change the constructor to be package-private",
                                            classifier, bean)));
                }
            }

            // transform any private injected fields into package private
            if (bean.getDeployment().transformPrivateInjectedFields) {
                for (Injection injection : bean.getInjections()) {
                    if (injection.isField() && Modifier.isPrivate(injection.getTarget().asField().flags())) {
                        bytecodeTransformerConsumer.accept(new BytecodeTransformer(beanClass.name().toString(),
                                new PrivateInjectedFieldTransformFunction(injection.getTarget().asField().name())));
                    }
                }
            }

        } else if (bean.isProducer()) {
            String methodOrField = bean.isProducerMethod() ? "method" : "field";
            String classifier = "Producer " + methodOrField + " for a normal scoped bean";

            Type type = bean.isProducerMethod() ? bean.getTarget().get().asMethod().returnType()
                    : bean.getTarget().get().asField().type();
            if (bean.getScope().isNormal()) {
                if (type.kind() == Kind.PRIMITIVE) {
                    errors.add(new DeploymentException(String.format("%s must not have a primitive type", classifier)));
                } else if (type.kind() == Kind.ARRAY) {
                    errors.add(new DeploymentException(String.format("%s must not have an array type", classifier)));
                }
            }

            ClassInfo returnTypeClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(), type);
            // null for primitive or array types, but those are covered above
            if (returnTypeClass != null && bean.getScope().isNormal() && !Modifier.isInterface(returnTypeClass.flags())) {
                if (Modifier.isFinal(returnTypeClass.flags())) {
                    if (bean.getDeployment().transformUnproxyableClasses) {
                        bytecodeTransformerConsumer
                                .accept(new BytecodeTransformer(returnTypeClass.name().toString(),
                                        new FinalClassTransformFunction()));
                    } else {
                        errors.add(
                                new DeploymentException(String.format("%s must not have a" +
                                        " return type that is final: %s", classifier, bean)));
                    }
                }
                if (bean.getDeployment().strictCompatibility) {
                    validateNonStaticFinalMethods(returnTypeClass, bean.getDeployment().getBeanArchiveIndex(),
                            classifier, errors);
                }
                MethodInfo noArgsConstructor = returnTypeClass.method(Methods.INIT);
                if (noArgsConstructor == null) {
                    if (bean.getDeployment().transformUnproxyableClasses) {
                        DotName superName = returnTypeClass.superName();
                        if (!DotNames.OBJECT.equals(superName)) {
                            ClassInfo superClass = bean.getDeployment().getBeanArchiveIndex()
                                    .getClassByName(returnTypeClass.superName());
                            if (superClass == null || !superClass.hasNoArgsConstructor()) {
                                // Bean class extends a class without no-args constructor
                                // It is not possible to generate a no-args constructor reliably
                                superName = null;
                            }
                        }
                        if (superName != null) {
                            if (!classesReceivingNoArgsCtor.contains(returnTypeClass.name())) {
                                String superClassName = superName.toString().replace('.', '/');
                                bytecodeTransformerConsumer.accept(new BytecodeTransformer(returnTypeClass.name().toString(),
                                        new NoArgConstructorTransformFunction(superClassName)));
                                classesReceivingNoArgsCtor.add(returnTypeClass.name());
                            }
                        } else {
                            errors.add(cannotAddSyntheticNoArgsConstructor(returnTypeClass));
                        }
                    } else {
                        errors.add(new DefinitionException(String
                                .format("Return type of a producer %s for normal scoped beans must" +
                                        " declare a non-private constructor with no parameters: %s", methodOrField, bean)));
                    }
                } else if (Modifier.isPrivate(noArgsConstructor.flags())) {
                    if (bean.getDeployment().transformUnproxyableClasses) {
                        bytecodeTransformerConsumer.accept(
                                new BytecodeTransformer(returnTypeClass.name().toString(),
                                        new PrivateNoArgsConstructorTransformFunction()));
                    } else {
                        errors.add(
                                new DeploymentException(
                                        String.format(
                                                "%s is not proxyable because it has a private no-args constructor: %s.",
                                                classifier, bean)));
                    }
                }
            }
        } else if (bean.isSynthetic()) {
            // synth beans can accidentally be defined with a non-existing scope, throw exception in such case
            DotName scopeName = bean.getScope().getDotName();
            if (bean.getDeployment().getScope(scopeName) == null) {
                throw new IllegalArgumentException("A synthetic bean " + bean + " was defined with invalid scope annotation - "
                        + scopeName + ". Please use one of the built-in scopes or a valid, registered custom scope.");
            }
            // this is for synthetic beans that need to be proxied but their classes don't have no-args constructor
            ClassInfo beanClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(), bean.getBeanClass());
            MethodInfo noArgsConstructor = beanClass.method(Methods.INIT);
            if (bean.getScope().isNormal() && !Modifier.isInterface(beanClass.flags()) && noArgsConstructor == null) {
                if (bean.getDeployment().transformUnproxyableClasses) {
                    DotName superName = beanClass.superName();
                    if (!DotNames.OBJECT.equals(superName)) {
                        ClassInfo superClass = bean.getDeployment().getBeanArchiveIndex().getClassByName(beanClass.superName());
                        if (superClass == null || !superClass.hasNoArgsConstructor()) {
                            // Bean class extends a class without no-args constructor
                            // It is not possible to generate a no-args constructor reliably
                            superName = null;
                        }
                    }
                    if (superName != null) {
                        if (!classesReceivingNoArgsCtor.contains(beanClass.name())) {
                            String superClassName = superName.toString().replace('.', '/');
                            bytecodeTransformerConsumer.accept(new BytecodeTransformer(beanClass.name().toString(),
                                    new NoArgConstructorTransformFunction(superClassName)));
                            classesReceivingNoArgsCtor.add(beanClass.name());
                        }
                    } else {
                        errors.add(cannotAddSyntheticNoArgsConstructor(beanClass));
                    }
                } else {
                    errors.add(new DeploymentException(String
                            .format("Normal scoped beans must declare a non-private constructor with no parameters: %s",
                                    bean)));
                }
            }
            if (bean.getScope().isNormal() && !Modifier.isInterface(beanClass.flags())
                    && bean.getDeployment().strictCompatibility) {
                validateNonStaticFinalMethods(beanClass, bean.getDeployment().getBeanArchiveIndex(), "Normal scoped", errors);
            }
        }
    }

    private static void validateNonStaticFinalMethods(ClassInfo clazz, IndexView beanArchiveIndex,
            String classifier, List<Throwable> errors) {
        // see also Methods.skipForClientProxy()
        while (!clazz.name().equals(DotNames.OBJECT)) {
            for (MethodInfo method : clazz.methods()) {
                if (Methods.IGNORED_METHODS.contains(method.name()) // constructor or static init
                        || Modifier.isStatic(method.flags())
                        || Modifier.isPrivate(method.flags())
                        || method.isSynthetic()) {
                    continue;
                }

                if (Modifier.isFinal(method.flags())) {
                    errors.add(new DeploymentException(String.format(
                            "%s bean must not declare non-static final methods with public, protected or default visibility: %s",
                            classifier, method)));
                }
            }

            ClassInfo superClass = getClassByName(beanArchiveIndex, clazz.superName());
            if (superClass == null) {
                break;
            }
            clazz = superClass;
        }
    }

    private static DeploymentException cannotAddSyntheticNoArgsConstructor(ClassInfo beanClass) {
        String message = "It's not possible to automatically add a synthetic no-args constructor to an unproxyable bean class. You need to manually add a non-private no-args constructor to %s in order to fulfill the requirements for normal scoped/intercepted/decorated beans.";
        return new DeploymentException(String.format(message, beanClass));
    }

    private static void fetchType(Type type, BeanDeployment beanDeployment) {
        if (type == null) {
            return;
        }
        if (type.kind() == Type.Kind.CLASS) {
            // Index the class additionally if needed
            getClassByName(beanDeployment.getBeanArchiveIndex(), type.name());
        } else {
            analyzeType(type, beanDeployment);
        }
    }

    private static void collectCallbacks(ClassInfo clazz, List<MethodInfo> callbacks, DotName annotation, IndexView index,
            Set<String> knownMethods) {
        for (MethodInfo method : clazz.methods()) {
            if (method.hasAnnotation(annotation) && !knownMethods.contains(method.name())) {
                if (method.returnType().kind() == Kind.VOID && method.parameterTypes().isEmpty()) {
                    callbacks.add(method);
                } else {
                    // invalid signature - build a meaningful message.
                    throw new DefinitionException("Invalid signature for the method `" + method + "` from class `"
                            + method.declaringClass() + "`. Methods annotated with `" + annotation + "` must return" +
                            " `void` and cannot have parameters.");
                }
            }
            knownMethods.add(method.name());
        }
        if (clazz.superName() != null) {
            ClassInfo superClass = getClassByName(index, clazz.superName());
            if (superClass != null) {
                collectCallbacks(superClass, callbacks, annotation, index, knownMethods);
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

    private static Integer initAlternativePriority(AnnotationTarget target, Integer alternativePriority,
            List<StereotypeInfo> stereotypes, BeanDeployment deployment) {
        if (alternativePriority == null) {
            // No @Priority or @AlernativePriority used - try stereotypes
            alternativePriority = initStereotypeAlternativePriority(stereotypes, target, deployment);
        }
        Integer computedPriority = deployment.computeAlternativePriority(target, stereotypes);
        if (computedPriority != null) {
            if (alternativePriority != null) {
                LOGGER.infof(
                        "Computed priority [%s] overrides the priority [%s] declared via @Priority",
                        computedPriority, alternativePriority);
            }
            alternativePriority = computedPriority;
        }
        return alternativePriority;
    }

    static class FinalClassTransformFunction implements BiFunction<String, ClassVisitor, ClassVisitor> {

        @Override
        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                @Override
                public void visit(int version, int access, String name, String signature,
                        String superName,
                        String[] interfaces) {
                    LOGGER.debugf("Final flag removed from bean class %s", className);
                    super.visit(version, access = access & (~Opcodes.ACC_FINAL), name, signature,
                            superName, interfaces);
                }
            };
        }
    }

    static class NoArgConstructorTransformFunction implements BiFunction<String, ClassVisitor, ClassVisitor> {

        private final String superClassName;

        public NoArgConstructorTransformFunction(String superClassName) {
            this.superClassName = superClassName;
        }

        @Override
        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                @Override
                public void visit(int version, int access, String name, String signature,
                        String superName,
                        String[] interfaces) {
                    super.visit(version, access, name, signature, superName, interfaces);
                    MethodVisitor mv = visitMethod(Modifier.PUBLIC | Opcodes.ACC_SYNTHETIC, Methods.INIT, "()V", null,
                            null);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, Methods.INIT, "()V",
                            false);
                    // NOTE: it seems that we do not need to handle final fields?
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(1, 1);
                    mv.visitEnd();
                    LOGGER.debugf("Added a no-args constructor to bean class: %s", className);
                }
            };
        }

    }

    static class PrivateNoArgsConstructorTransformFunction implements BiFunction<String, ClassVisitor, ClassVisitor> {

        @Override
        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                    if (name.equals(Methods.INIT)) {
                        access = access & (~Opcodes.ACC_PRIVATE);
                        LOGGER.debugf(
                                "Changed visibility of a private no-args constructor to package-private: %s",
                                className);
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            };
        }

    }

    // alters an injected field modifier from private to package private
    static class PrivateInjectedFieldTransformFunction implements BiFunction<String, ClassVisitor, ClassVisitor> {

        public PrivateInjectedFieldTransformFunction(String fieldName) {
            this.fieldName = fieldName;
        }

        private String fieldName;

        @Override
        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                @Override
                public FieldVisitor visitField(
                        int access,
                        String name,
                        String descriptor,
                        String signature,
                        Object value) {
                    if (name.equals(fieldName)) {
                        access = access & (~Opcodes.ACC_PRIVATE);
                        LOGGER.debugf(
                                "Changed visibility of an injected private field to package-private. Field name: %s in class: %s",
                                name, className);
                    }
                    return super.visitField(access, name, descriptor, signature, value);
                }
            };
        }

    }

    private static class ClassBeanFactory {

        private final ClassInfo beanClass;
        private final BeanDeployment beanDeployment;
        private final InjectionPointModifier transformer;

        private String name;
        private Integer priority;
        private boolean isAlternative;
        private boolean isDefaultBean;

        ClassBeanFactory(ClassInfo beanClass, BeanDeployment beanDeployment, InjectionPointModifier transformer) {
            this.beanClass = beanClass;
            this.beanDeployment = beanDeployment;
            this.transformer = transformer;
            this.priority = null;
            this.isAlternative = false;
            this.isDefaultBean = false;
            this.name = null;
        }

        void processInheritedAnnotation(
                AnnotationInstance annotation,
                BeanDeployment beanDeployment,
                Set<AnnotationInstance> qualifiers,
                List<StereotypeInfo> stereotypes) {
            Collection<AnnotationInstance> qualifierCollection = beanDeployment.extractQualifiers(annotation);
            for (AnnotationInstance qualifierAnnotation : qualifierCollection) {
                if (beanDeployment.isInheritedQualifier(qualifierAnnotation.name())
                        && !Annotations.contains(qualifiers, qualifierAnnotation.name())) {
                    qualifiers.add(qualifierAnnotation);
                }
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotation.name());
            if (stereotype != null) {
                if (stereotype.isInherited()) {
                    stereotypes.add(stereotype);
                }
            }
        }

        void processAnnotation(AnnotationInstance annotation,
                ClassInfo beanClass,
                BeanDeployment beanDeployment,
                Set<AnnotationInstance> qualifiers,
                List<StereotypeInfo> stereotypes,
                List<ScopeInfo> scopes,
                Set<ScopeInfo> beanDefiningAnnotationScopes) {
            DotName annotationName = annotation.name();
            if (DotNames.NAMED.equals(annotationName)) {
                AnnotationValue nameValue = annotation.value();
                if (nameValue != null) {
                    name = nameValue.asString();
                } else {
                    name = getDefaultName(beanClass);
                    annotation = normalizedNamedQualifier(name, annotation);
                }
            }
            BeanDefiningAnnotation bda = beanDeployment.getBeanDefiningAnnotation(annotationName);
            if (bda != null && bda.getDefaultScope() != null) {
                beanDefiningAnnotationScopes.add(beanDeployment.getScope(bda.getDefaultScope()));
            }
            // Qualifiers
            Collection<AnnotationInstance> qualifierCollection = beanDeployment.extractQualifiers(annotation);
            for (AnnotationInstance qualifierAnnotation : qualifierCollection) {
                qualifiers.add(qualifierAnnotation);
            }
            if (!qualifierCollection.isEmpty()) {
                // we needn't process it further, the annotation was a qualifier (or multiple repeating ones)
                return;
            }
            if (DotNames.ALTERNATIVE.equals(annotationName)) {
                isAlternative = true;
                return;
            }
            if (DotNames.DEFAULT_BEAN.equals(annotationName)) {
                isDefaultBean = true;
                return;
            }
            if (DotNames.PRIORITY.equals(annotationName)) {
                priority = annotation.value().asInt();
                return;
            }
            if (priority == null && DotNames.ARC_PRIORITY.equals(annotationName)) {
                priority = annotation.value().asInt();
                return;
            }
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotationName);
            if (stereotype != null) {
                stereotypes.add(stereotype);
                return;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotationName);
            if (scopeAnnotation != null) {
                if (!scopes.contains(scopeAnnotation)) {
                    scopes.add(scopeAnnotation);
                }
            }
        }

        BeanInfo create() {
            Set<AnnotationInstance> qualifiers = new HashSet<>();
            List<ScopeInfo> scopes = new ArrayList<>();
            Set<Type> types = Types.getClassBeanTypeClosure(beanClass, beanDeployment);

            List<StereotypeInfo> stereotypes = new ArrayList<>();
            Collection<AnnotationInstance> annotations = beanDeployment.getAnnotations(beanClass);
            Set<ScopeInfo> beanDefiningAnnotationScopes = new HashSet<>();

            for (AnnotationInstance annotation : annotations) {
                processAnnotation(annotation, beanClass, beanDeployment, qualifiers, stereotypes, scopes,
                        beanDefiningAnnotationScopes);
            }
            processSuperClass(beanClass, beanDeployment, qualifiers, stereotypes);

            if (scopes.size() > 1) {
                throw multipleScopesFound("Bean class " + beanClass, scopes);
            }
            // 1. Explicit scope (including inherited one)
            // 2. Stereotype scope
            // 3. Bean defining annotation default scope
            ScopeInfo scope;
            if (scopes.isEmpty()) {
                // Inheritance of type-level metadata: "A scope type explicitly declared by X and inherited by Y from X takes precedence over default scopes of stereotypes declared or inherited by Y."
                scope = inheritScope(beanClass, beanDeployment);
                if (scope == null) {
                    scope = initStereotypeScope(stereotypes, beanClass, beanDeployment);
                    if (scope == null) {
                        scope = initBeanDefiningAnnotationScope(beanDefiningAnnotationScopes, beanClass);
                    }
                }
            } else {
                scope = scopes.get(0);
            }
            if (scope != null // `null` is just like `@Dependent`
                    && !BuiltinScope.DEPENDENT.is(scope)
                    && !beanClass.typeParameters().isEmpty()) {
                throw new DefinitionException(
                        "Declaring class of a managed bean is generic, its scope must be @Dependent: " + beanClass);
            }
            if (!isAlternative) {
                isAlternative = initStereotypeAlternative(stereotypes, beanDeployment);
            }
            if (name == null) {
                name = initStereotypeName(stereotypes, beanClass, beanDeployment);
            }

            if (isAlternative) {
                priority = initAlternativePriority(beanClass, priority, stereotypes, beanDeployment);
                if (priority == null) {
                    // after all attempts, priority is still null, bean will be ignored
                    LOGGER.debugf(
                            "Ignoring bean defined via %s - declared as an @Alternative but not selected by @Priority or quarkus.arc.selected-alternatives",
                            beanClass.name());
                    return null;
                }
            }

            List<Injection> injections = Injection.forBean(beanClass, null, beanDeployment, transformer,
                    Injection.BeanType.MANAGED_BEAN);
            BeanInfo bean = new BeanInfo(beanClass, beanDeployment, scope, types, qualifiers,
                    injections, null, null, isAlternative, stereotypes, name, isDefaultBean, null, priority);
            for (Injection injection : injections) {
                injection.init(bean);
            }
            return bean;
        }

        void processSuperClass(ClassInfo beanClass, BeanDeployment beanDeployment,
                Set<AnnotationInstance> qualifiers, List<StereotypeInfo> stereotypes) {
            DotName superClassName = beanClass.superName();
            while (!superClassName.equals(DotNames.OBJECT)) {
                ClassInfo classFromIndex = getClassByName(beanDeployment.getBeanArchiveIndex(), superClassName);
                if (classFromIndex == null) {
                    // class not in index
                    LOGGER.warnf("Unable to get inherited qualifier of stereotype for bean %s because its " +
                            "super class %s is not part of Jandex index. This inheritance will not apply.",
                            beanClass,
                            superClassName);
                    break;
                }
                for (AnnotationInstance annotation : beanDeployment.getAnnotationStore().getAnnotations(classFromIndex)) {
                    processInheritedAnnotation(annotation, beanDeployment, qualifiers, stereotypes);
                }
                superClassName = classFromIndex.superName();
            }
        }
    }

}
