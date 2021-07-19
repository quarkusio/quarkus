package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;
import io.quarkus.arc.processor.InjectionTargetInfo.TargetKind;
import io.quarkus.gizmo.Gizmo;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class Beans {

    static final Logger LOGGER = Logger.getLogger(Beans.class);

    private Beans() {
    }

    static BeanInfo createClassBean(ClassInfo beanClass, BeanDeployment beanDeployment, InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getClassBeanTypeClosure(beanClass, beanDeployment);
        Integer alternativePriority = null;
        boolean isAlternative = false;
        boolean isDefaultBean = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(beanClass)) {
            if (DotNames.NAMED.equals(annotation.name())) {
                AnnotationValue nameValue = annotation.value();
                if (nameValue != null) {
                    name = nameValue.asString();
                } else {
                    name = getDefaultName(beanClass);
                    annotation = normalizedNamedQualifier(name, annotation);
                }
            }
            Collection<AnnotationInstance> qualifierCollection = beanDeployment.extractQualifiers(annotation);
            for (AnnotationInstance qualifierAnnotation : qualifierCollection) {
                // Qualifiers
                qualifiers.add(qualifierAnnotation);
            }
            // Treat the case when an additional bean defining annotation that is also a qualifier declares the default scope
            StereotypeInfo stereotype = beanDeployment.getStereotype(annotation.name());
            if (stereotype != null) {
                stereotypes.add(stereotype);
                continue;
            }
            if (!qualifierCollection.isEmpty()) {
                // we needn't process it further, the annotation was a qualifier (or multiple repeating ones)
                continue;
            }
            if (annotation.name()
                    .equals(DotNames.ALTERNATIVE)) {
                isAlternative = true;
                continue;
            }
            if (annotation.name()
                    .equals(DotNames.ALTERNATIVE_PRIORITY)) {
                isAlternative = true;
                alternativePriority = annotation.value().asInt();
                continue;
            }
            if (DotNames.DEFAULT_BEAN.equals(annotation.name())) {
                isDefaultBean = true;
                continue;
            }
            if (annotation.name()
                    .equals(DotNames.PRIORITY) && alternativePriority == null) {
                alternativePriority = annotation.value()
                        .asInt();
                continue;
            }
            ScopeInfo scopeAnnotation = beanDeployment.getScope(annotation.name());
            if (scopeAnnotation != null) {
                scopes.add(scopeAnnotation);
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

        if (isAlternative) {
            alternativePriority = initAlternativePriority(beanClass, alternativePriority, stereotypes, beanDeployment);
            if (alternativePriority == null) {
                // after all attempts, priority is still null, bean will be ignored
                LOGGER.debugf(
                        "Ignoring bean defined via %s - declared as an @Alternative but not selected by @Priority, @AlternativePriority or quarkus.arc.selected-alternatives",
                        beanClass.name());
                return null;
            }
        }

        BeanInfo bean = new BeanInfo(beanClass, beanDeployment, scope, types, qualifiers,
                Injection.forBean(beanClass, null, beanDeployment, transformer), null, null,
                isAlternative ? alternativePriority : null, stereotypes, name, isDefaultBean);
        return bean;
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

    static BeanInfo createProducerMethod(MethodInfo producerMethod, BeanInfo declaringBean, BeanDeployment beanDeployment,
            DisposerInfo disposer, InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getProducerMethodTypeClosure(producerMethod, beanDeployment);
        Integer alternativePriority = null;
        boolean isAlternative = false;
        boolean isDefaultBean = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(producerMethod)) {
            //only check for method annotations since at this point we will get both
            // method and method param annotations
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (DotNames.NAMED.equals(annotation.name())) {
                AnnotationValue nameValue = annotation.value();
                if (nameValue != null) {
                    name = nameValue.asString();
                } else {
                    name = getDefaultName(producerMethod);
                    annotation = normalizedNamedQualifier(name, annotation);
                }
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
            if (DotNames.ALTERNATIVE.equals(annotation.name())) {
                isAlternative = true;
                continue;
            }
            if (DotNames.ALTERNATIVE_PRIORITY.equals(annotation.name())) {
                isAlternative = true;
                alternativePriority = annotation.value().asInt();
                continue;
            }
            if (DotNames.DEFAULT_BEAN.equals(annotation.name())) {
                isDefaultBean = true;
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
            if (alternativePriority == null) {
                alternativePriority = declaringBean.getAlternativePriority();
            }
            alternativePriority = initAlternativePriority(producerMethod, alternativePriority, stereotypes, beanDeployment);
            if (alternativePriority == null) {
                // after all attempts, priority is still null, bean will be ignored
                LOGGER.debugf(
                        "Ignoring producer method %s - declared as an @Alternative but not selected by @AlternativePriority or quarkus.arc.selected-alternatives",
                        declaringBean.getTarget().get().asClass().name() + "#" + producerMethod.name());
                return null;
            }
        }

        BeanInfo bean = new BeanInfo(producerMethod, beanDeployment, scope, types, qualifiers,
                Injection.forBean(producerMethod, declaringBean, beanDeployment, transformer), declaringBean,
                disposer, alternativePriority, stereotypes, name, isDefaultBean);
        return bean;
    }

    static BeanInfo createProducerField(FieldInfo producerField, BeanInfo declaringBean, BeanDeployment beanDeployment,
            DisposerInfo disposer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<ScopeInfo> scopes = new ArrayList<>();
        Set<Type> types = Types.getProducerFieldTypeClosure(producerField, beanDeployment);
        Integer alternativePriority = null;
        boolean isAlternative = false;
        boolean isDefaultBean = false;
        List<StereotypeInfo> stereotypes = new ArrayList<>();
        String name = null;

        for (AnnotationInstance annotation : beanDeployment.getAnnotations(producerField)) {
            if (DotNames.NAMED.equals(annotation.name())) {
                AnnotationValue nameValue = annotation.value();
                if (nameValue != null) {
                    name = nameValue.asString();
                } else {
                    name = producerField.name();
                    annotation = normalizedNamedQualifier(name, annotation);
                }
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
            if (DotNames.ALTERNATIVE.equals(annotation.name())) {
                isAlternative = true;
                continue;
            }
            if (DotNames.ALTERNATIVE_PRIORITY.equals(annotation.name())) {
                isAlternative = true;
                alternativePriority = annotation.value().asInt();
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
            if (DotNames.DEFAULT_BEAN.equals(annotation.name())) {
                isDefaultBean = true;
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
            if (alternativePriority == null) {
                alternativePriority = declaringBean.getAlternativePriority();
            }
            alternativePriority = initAlternativePriority(producerField, alternativePriority, stereotypes, beanDeployment);
            // after all attempts, priority is still null
            if (alternativePriority == null) {
                LOGGER.debugf(
                        "Ignoring producer field %s - declared as an @Alternative but not selected by @AlternativePriority or quarkus.arc.selected-alternatives",
                        producerField);
                return null;
            }
        }

        BeanInfo bean = new BeanInfo(producerField, beanDeployment, scope, types, qualifiers, Collections.emptyList(),
                declaringBean, disposer, alternativePriority, stereotypes, name, isDefaultBean);
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

    private static ScopeInfo initStereotypeScope(List<StereotypeInfo> stereotypes, AnnotationTarget target) {
        if (stereotypes.isEmpty()) {
            return null;
        }
        final Set<ScopeInfo> stereotypeScopes = new HashSet<>();
        final Set<ScopeInfo> additionalBDAScopes = new HashSet<>();
        for (StereotypeInfo stereotype : stereotypes) {
            if (!stereotype.isAdditionalBeanDefiningAnnotation()) {
                stereotypeScopes.add(stereotype.getDefaultScope());
            } else {
                additionalBDAScopes.add(stereotype.getDefaultScope());
            }
        }
        // if the stereotypeScopes set is empty, operate on additional BDA stereotypes instead
        return BeanDeployment.getValidScope(stereotypeScopes.isEmpty() ? additionalBDAScopes : stereotypeScopes, target);
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

    private static Integer initStereotypeAlternativePriority(List<StereotypeInfo> stereotypes) {
        if (stereotypes.isEmpty()) {
            return null;
        }
        for (StereotypeInfo stereotype : stereotypes) {
            if (stereotype.getAlternativePriority() != null) {
                return stereotype.getAlternativePriority();
            }
        }
        return null;
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
        return matches(bean, typeAndQualifiers.type, typeAndQualifiers.qualifiers);
    }

    static boolean matches(BeanInfo bean, Type requiredType, Set<AnnotationInstance> requiredQualifiers) {
        // Bean has all the required qualifiers and a bean type that matches the required type
        return hasQualifiers(bean, requiredQualifiers) && matchesType(bean, requiredType);
    }

    static boolean matchesType(BeanInfo bean, Type requiredType) {
        BeanResolverImpl beanResolver = bean.getDeployment().beanResolver;
        for (Type beanType : bean.getTypes()) {
            if (beanResolver.matches(requiredType, beanType)) {
                return true;
            }
        }
        return false;
    }

    static void resolveInjectionPoint(BeanDeployment deployment, InjectionTargetInfo target, InjectionPointInfo injectionPoint,
            List<Throwable> errors) {
        if (injectionPoint.isDelegate()) {
            // Skip delegate injection points
            return;
        }
        BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
        if (builtinBean != null) {
            if (BuiltinBean.INJECTION_POINT == builtinBean
                    && (target.kind() != TargetKind.BEAN || !BuiltinScope.DEPENDENT.is(target.asBean().getScope()))) {
                errors.add(new DefinitionException("Only @Dependent beans can access metadata about an injection point: "
                        + injectionPoint.getTargetInfo()));
            } else if (BuiltinBean.EVENT_METADATA == builtinBean
                    && target.kind() != TargetKind.OBSERVER) {
                errors.add(new DefinitionException("EventMetadata can be only injected into an observer method: "
                        + injectionPoint.getTargetInfo()));
            } else if (BuiltinBean.INSTANCE == builtinBean
                    && injectionPoint.getType().kind() != Kind.PARAMETERIZED_TYPE) {
                errors.add(
                        new DefinitionException("An injection point of raw type javax.enterprise.inject.Instance is defined: "
                                + injectionPoint.getTargetInfo()));
            }
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
        message.append("\n\t- java member: ");
        message.append(injectionPoint.getTargetInfo());
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
        List<AnnotationValue> values = new ArrayList<>();
        Set<String> nonBindingFields = beanDeployment.getQualifierNonbindingMembers(requiredQualifier.name());
        for (AnnotationValue val : requiredQualifier.values()) {
            if (!requiredClazz.method(val.name()).hasAnnotation(DotNames.NONBINDING)
                    && !nonBindingFields.contains(val.name())) {
                values.add(val);
            }
        }
        for (AnnotationInstance qualifier : qualifiers) {
            if (requiredQualifier.name().equals(qualifier.name())) {
                // Must have the same annotation member value for each member which is not annotated @Nonbinding
                boolean matches = true;
                for (AnnotationValue value : values) {
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

    static void validateBean(BeanInfo bean, List<Throwable> errors, List<BeanDeploymentValidator> validators,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, Set<DotName> classesReceivingNoArgsCtor) {

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

        } else if (bean.isProducerField() || bean.isProducerMethod()) {
            ClassInfo returnTypeClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(),
                    bean.isProducerMethod() ? bean.getTarget().get().asMethod().returnType()
                            : bean.getTarget().get().asField().type());
            // can be null for primitive types
            if (returnTypeClass != null && bean.getScope().isNormal() && !Modifier.isInterface(returnTypeClass.flags())) {
                String methodOrField = bean.isProducerMethod() ? "method" : "field";
                String classifier = "Producer " + methodOrField + " for a normal scoped bean";
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

    private static void collectCallbacks(ClassInfo clazz, List<MethodInfo> callbacks, DotName annotation, IndexView index) {
        for (MethodInfo method : clazz.methods()) {
            if (method.hasAnnotation(annotation) && method.returnType().kind() == Kind.VOID && method.parameters().isEmpty()) {
                callbacks.add(method);
            }
        }
        if (clazz.superName() != null) {
            ClassInfo superClass = getClassByName(index, clazz.superName());
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

    private static Integer initAlternativePriority(AnnotationTarget target, Integer alternativePriority,
            List<StereotypeInfo> stereotypes, BeanDeployment deployment) {
        if (alternativePriority == null) {
            // No @Priority or @AlernativePriority used - try stereotypes
            alternativePriority = initStereotypeAlternativePriority(stereotypes);
        }
        Integer computedPriority = deployment.computeAlternativePriority(target, stereotypes);
        if (computedPriority != null) {
            if (alternativePriority != null) {
                LOGGER.infof(
                        "Computed priority [%s] overrides the priority [%s] declared via @Priority or @AlernativePriority",
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
                    MethodVisitor mv = visitMethod(Modifier.PUBLIC, Methods.INIT, "()V", null,
                            null);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, Methods.INIT, "()V",
                            false);
                    // NOTE: it seems that we do not need to handle final fields?
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(1, 1);
                    mv.visitEnd();
                    LOGGER.debugf("Added a no-args constructor to bean class: ", className);
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
                                "Changed visibility of a private no-args constructor to package-private: ",
                                className);
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            };
        }

    }

}
