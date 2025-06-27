package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;

/**
 * Injection abstraction, basically a collection of injection points plus the annotation target:
 * <ul>
 * <li>an injected field,</li>
 * <li>a bean constructor,</li>
 * <li>an initializer method,</li>
 * <li>a producer method,</li>
 * <li>a disposer method,</li>
 * <li>an observer method,</li>
 * <li>a managed bean method for which an invoker with argument lookups is created.</li>
 * </ul>
 *
 * @author Martin Kouba
 */
public class Injection {

    private static final Logger LOGGER = Logger.getLogger(Injection.class);

    static Injection forSyntheticBean(Iterable<TypeAndQualifiers> injectionPoints) {
        return forSynthetic(injectionPoints, BeanType.SYNTHETIC_BEAN);
    }

    static Injection forSyntheticInterceptor(Iterable<TypeAndQualifiers> injectionPoints) {
        return forSynthetic(injectionPoints, BeanType.SYNTHETIC_INTERCEPTOR);
    }

    private static Injection forSynthetic(Iterable<TypeAndQualifiers> injectionPoints, BeanType beanType) {
        List<InjectionPointInfo> ret = new ArrayList<>();
        for (TypeAndQualifiers injectionPoint : injectionPoints) {
            InjectionPointInfo ip = InjectionPointInfo.fromSyntheticInjectionPoint(injectionPoint);
            validateInjections(ip, beanType);
            ret.add(ip);
        }
        return new Injection(null, ret);
    }

    private static void validateInjections(InjectionPointInfo injectionPointInfo, BeanType beanType) {
        // Mostly validation related to Bean metadata injection restrictions
        // see https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#bean_metadata
        if (beanType == BeanType.MANAGED_BEAN || beanType == BeanType.SYNTHETIC_BEAN || beanType == BeanType.PRODUCER_METHOD) {
            // If an Interceptor<T> instance is injected into a bean instance other than an interceptor instance,
            // the container automatically detects the problem and treats it as a definition error.
            if (injectionPointInfo.getType().name().equals(DotNames.INTERCEPTOR_BEAN)) {
                throw new DefinitionException("Invalid injection of Interceptor<T> bean, can only be used in interceptors " +
                        "but was detected in: " + injectionPointInfo.getTargetInfo());
            }

            // If a Bean instance with qualifier @Intercepted is injected into a bean instance other than an interceptor
            // instance, the container automatically detects the problem and treats it as a definition error.
            if (injectionPointInfo.getType().name().equals(DotNames.BEAN)
                    && injectionPointInfo.getRequiredQualifier(DotNames.INTERCEPTED) != null) {
                throw new DefinitionException(
                        "Invalid injection of @Intercepted Bean<T>, can only be injected into interceptors " +
                                "but was detected in: " + injectionPointInfo.getTargetInfo());
            }

            // If a Decorator<T> instance is injected into a bean instance other than a decorator instance,
            // the container automatically detects the problem and treats it as a definition error.
            if (injectionPointInfo.getType().name().equals(DotNames.DECORATOR)) {
                throw new DefinitionException("Invalid injection of Decorator<T> bean, can only be used in decorators " +
                        "but was detected in: " + injectionPointInfo.getTargetInfo());
            }

            // If a Bean instance with qualifier @Decorated is injected into a bean instance other than a decorator
            // instance, the container automatically detects the problem and treats it as a definition error.
            if (injectionPointInfo.getType().name().equals(DotNames.BEAN)
                    && injectionPointInfo.getRequiredQualifier(DotNames.DECORATED) != null) {
                throw new DefinitionException(
                        "Invalid injection of @Decorated Bean<T>, can only be injected into decorators " +
                                "but was detected in: " + injectionPointInfo.getTargetInfo());
            }

            // the injection point is a field, an initializer method parameter or a bean constructor, with qualifier
            // @Default, then the type parameter of the injected Bean, or Interceptor must be the same as the type
            // declaring the injection point
            if (injectionPointInfo.getRequiredType().name().equals(DotNames.BEAN)
                    && injectionPointInfo.getRequiredType().kind() == Type.Kind.PARAMETERIZED_TYPE
                    && injectionPointInfo.getRequiredType().asParameterizedType().arguments().size() == 1
                    && injectionPointInfo.hasDefaultedQualifier()) {
                Type actualType = injectionPointInfo.getRequiredType().asParameterizedType().arguments().get(0);
                AnnotationTarget ipTarget = injectionPointInfo.getAnnotationTarget();
                DotName expectedType = null;
                if (ipTarget.kind() == Kind.FIELD) {
                    // field injection derives this from the class
                    expectedType = ipTarget.asField().declaringClass().name();
                } else if (ipTarget.kind() == Kind.METHOD_PARAMETER) {
                    // the injection point is a producer method parameter then the type parameter of the injected Bean
                    // must be the same as the producer method return type
                    if (beanType == BeanType.PRODUCER_METHOD) {
                        expectedType = ipTarget.asMethodParameter().method().returnType().name();
                    } else {
                        expectedType = ipTarget.asMethodParameter().method().declaringClass().name();
                    }
                }
                if (expectedType != null
                        // This is very rudimentary check, might need to be expanded?
                        && !expectedType.equals(actualType.name())) {
                    throw new DefinitionException(
                            "Type of injected Bean<T> does not match the type of the bean declaring the " +
                                    "injection point. Problematic injection point: " + injectionPointInfo.getTargetInfo());
                }
            }
        }
        if (beanType == BeanType.INTERCEPTOR) {
            // the injection point is a field, an initializer method parameter or a bean constructor of an interceptor,
            // with qualifier @Intercepted, then the type parameter of the injected Bean must be an unbounded wildcard
            if (injectionPointInfo.getRequiredType().name().equals(DotNames.BEAN)
                    && injectionPointInfo.getRequiredQualifier(DotNames.INTERCEPTED) != null
                    && injectionPointInfo.getRequiredType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
                ParameterizedType parameterizedType = injectionPointInfo.getRequiredType().asParameterizedType();
                // there should be exactly one param - wildcard - and it has to be unbound; all else is DefinitionException
                if (parameterizedType.arguments().size() != 1
                        || !(parameterizedType.arguments().get(0).kind() == Type.Kind.WILDCARD_TYPE)
                        || !(parameterizedType.arguments().get(0).asWildcardType().extendsBound().name().equals(DotNames.OBJECT)
                                && parameterizedType.arguments().get(0).asWildcardType().superBound() == null)) {
                    throw new DefinitionException(
                            "Injected @Intercepted Bean<?> has to use unbound wildcard as its type parameter. " +
                                    "Problematic injection point: " + injectionPointInfo.getTargetInfo());
                }
            }
            // the injection point is a field, an initializer method parameter or a bean constructor, with qualifier
            // @Default, then the type parameter of the injected Bean, or Interceptor must be the same as the type
            // declaring the injection point
            if (injectionPointInfo.getRequiredType().name().equals(DotNames.INTERCEPTOR_BEAN)
                    && injectionPointInfo.getRequiredType().kind() == Type.Kind.PARAMETERIZED_TYPE
                    && injectionPointInfo.getRequiredType().asParameterizedType().arguments().size() == 1) {
                Type actualType = injectionPointInfo.getRequiredType().asParameterizedType().arguments().get(0);
                AnnotationTarget ipTarget = injectionPointInfo.getAnnotationTarget();
                DotName expectedType = null;
                if (ipTarget.kind() == Kind.FIELD) {
                    expectedType = ipTarget.asField().declaringClass().name();
                } else if (ipTarget.kind() == Kind.METHOD_PARAMETER) {
                    expectedType = ipTarget.asMethodParameter().method().declaringClass().name();
                }
                if (expectedType != null
                        // This is very rudimentary check, might need to be expanded?
                        && !expectedType.equals(actualType.name())) {
                    throw new DefinitionException(
                            "Type of injected Interceptor<T> does not match the type of the bean declaring the " +
                                    "injection point. Problematic injection point: " + injectionPointInfo.getTargetInfo());
                }
            }
        }
        if (beanType == BeanType.DECORATOR) {
            // the injection point is a field, an initializer method parameter or a bean constructor, with qualifier
            // @Default, then the type parameter of the injected Decorator must be the same as the type
            // declaring the injection point
            if (injectionPointInfo.getRequiredType().name().equals(DotNames.DECORATOR)
                    && injectionPointInfo.getRequiredType().kind() == Type.Kind.PARAMETERIZED_TYPE
                    && injectionPointInfo.getRequiredType().asParameterizedType().arguments().size() == 1) {
                Type actualType = injectionPointInfo.getRequiredType().asParameterizedType().arguments().get(0);
                AnnotationTarget ipTarget = injectionPointInfo.getAnnotationTarget();
                DotName expectedType = null;
                if (ipTarget.kind() == Kind.FIELD) {
                    expectedType = ipTarget.asField().declaringClass().name();
                } else if (ipTarget.kind() == Kind.METHOD_PARAMETER) {
                    expectedType = ipTarget.asMethodParameter().method().declaringClass().name();
                }
                if (expectedType != null
                        // This is very rudimentary check, might need to be expanded?
                        && !expectedType.equals(actualType.name())) {
                    throw new DefinitionException(
                            "Type of injected Decorator<T> does not match the type of the bean declaring the " +
                                    "injection point. Problematic injection point: " + injectionPointInfo.getTargetInfo());
                }
            }

            // the injection point is a field, an initializer method parameter or a bean constructor of a decorator,
            // with qualifier @Decorated, then the type parameter of the injected Bean must be the same as the delegate type
            //
            // a validation for the specification text above would naturally belong here, but we don't have
            // access to the delegate type yet, so this is postponed to `Beans.validateInterceptorDecorator()`
        }
    }

    private static void validateInjections(List<Injection> injections, BeanType beanType) {
        for (Injection injection : injections) {
            for (InjectionPointInfo ipi : injection.injectionPoints) {
                validateInjections(ipi, beanType);
            }
        }
    }

    static List<Injection> forBean(AnnotationTarget beanTarget, BeanInfo declaringBean, BeanDeployment beanDeployment,
            InjectionPointModifier transformer, BeanType beanType) {
        if (Kind.CLASS.equals(beanTarget.kind())) {
            List<Injection> injections = forClassBean(beanTarget.asClass(), beanTarget.asClass(), beanDeployment,
                    transformer, false, new HashSet<>());

            Set<AnnotationTarget> injectConstructors = injections.stream().filter(Injection::isConstructor)
                    .map(Injection::getTarget).collect(Collectors.toSet());
            if (injectConstructors.size() > 1) {
                throw new DefinitionException(
                        "Multiple @Inject constructors found on " + beanTarget.asClass().name() + ":\n"
                                + injectConstructors.stream().map(Object::toString).collect(Collectors.joining("\n")));
            }
            for (AnnotationTarget injectConstructor : injectConstructors) {
                Set<AnnotationInstance> parameterAnnotations = Annotations.getParameterAnnotations(beanDeployment,
                        injectConstructor.asMethod());
                for (AnnotationInstance annotation : parameterAnnotations) {
                    if (DotNames.DISPOSES.equals(annotation.name())) {
                        throw new DefinitionException(
                                "Bean constructor must not have a @Disposes parameter: " + injectConstructor);
                    }
                    if (DotNames.OBSERVES.equals(annotation.name())) {
                        throw new DefinitionException(
                                "Bean constructor must not have an @Observes parameter: " + injectConstructor);
                    }
                    if (DotNames.OBSERVES_ASYNC.equals(annotation.name())) {
                        throw new DefinitionException(
                                "Bean constructor must not have an @ObservesAsync parameter: " + injectConstructor);
                    }
                }
            }

            Set<MethodInfo> initializerMethods = injections.stream()
                    .filter(it -> it.isMethod() && !it.isConstructor())
                    .map(Injection::getTarget)
                    .map(AnnotationTarget::asMethod)
                    .collect(Collectors.toSet());
            for (MethodInfo initializerMethod : initializerMethods) {
                if (beanDeployment.hasAnnotation(initializerMethod, DotNames.PRODUCES)) {
                    throw new DefinitionException("Initializer method must not be annotated @Produces "
                            + "(alternatively, producer method must not be annotated @Inject): "
                            + beanTarget.asClass() + "." + initializerMethod.name());
                }
                if (Annotations.hasParameterAnnotation(beanDeployment, initializerMethod, DotNames.DISPOSES)) {
                    throw new DefinitionException("Initializer method must not have a @Disposes parameter "
                            + "(alternatively, disposer method must not be annotated @Inject): "
                            + beanTarget.asClass() + "." + initializerMethod.name());
                }
                if (Annotations.hasParameterAnnotation(beanDeployment, initializerMethod, DotNames.OBSERVES)) {
                    throw new DefinitionException("Initializer method must not have an @Observes parameter "
                            + "(alternatively, observer method must not be annotated @Inject): "
                            + beanTarget.asClass() + "." + initializerMethod.name());
                }
                if (Annotations.hasParameterAnnotation(beanDeployment, initializerMethod, DotNames.OBSERVES_ASYNC)) {
                    throw new DefinitionException("Initializer method must not have an @ObservesAsync parameter "
                            + "(alternatively, async observer method must not be annotated @Inject): "
                            + beanTarget.asClass() + "." + initializerMethod.name());
                }
            }
            validateInjections(injections, beanType);
            return injections;
        } else if (Kind.METHOD.equals(beanTarget.kind())) {
            MethodInfo producerMethod = beanTarget.asMethod();

            if (beanDeployment.hasAnnotation(producerMethod, DotNames.INJECT)) {
                throw new DefinitionException("Producer method must not be annotated @Inject "
                        + "(alternatively, initializer method must not be annotated @Produces): "
                        + producerMethod);
            }
            if (Annotations.hasParameterAnnotation(beanDeployment, producerMethod, DotNames.DISPOSES)) {
                throw new DefinitionException("Producer method must not have a @Disposes parameter "
                        + "(alternatively, disposer method must not be annotated @Produces): "
                        + producerMethod);
            }
            if (Annotations.hasParameterAnnotation(beanDeployment, producerMethod, DotNames.OBSERVES)) {
                throw new DefinitionException("Producer method must not have an @Observes parameter "
                        + "(alternatively, observer method must not be annotated @Produces): "
                        + producerMethod);
            }
            if (Annotations.hasParameterAnnotation(beanDeployment, producerMethod, DotNames.OBSERVES_ASYNC)) {
                throw new DefinitionException("Producer method must not have an @ObservesAsync parameter "
                        + "(alternatively, async observer method must not be annotated @Produces): "
                        + producerMethod);
            }

            if (producerMethod.parameterTypes().isEmpty()) {
                return Collections.emptyList();
            }
            // All parameters are injection points
            List<Injection> injections = Collections.singletonList(new Injection(producerMethod,
                    InjectionPointInfo.fromMethod(producerMethod, declaringBean.getImplClazz(), beanDeployment, transformer)));
            validateInjections(injections, beanType);
            return injections;
        }
        throw new IllegalArgumentException("Unsupported annotation target");
    }

    static enum BeanType {
        MANAGED_BEAN,
        PRODUCER_METHOD,
        SYNTHETIC_BEAN,
        INTERCEPTOR,
        SYNTHETIC_INTERCEPTOR,
        DECORATOR
    }

    // returns injections in the order they should be performed
    private static List<Injection> forClassBean(ClassInfo beanClass, ClassInfo classInfo, BeanDeployment beanDeployment,
            InjectionPointModifier transformer, boolean skipConstructors, Set<MethodOverrideKey> seenMethods) {

        List<Injection> injections = new ArrayList<>();

        List<AnnotationInstance> injectAnnotations = getAllInjectionPoints(beanDeployment, classInfo, DotNames.INJECT,
                skipConstructors, seenMethods);

        for (AnnotationInstance injectAnnotation : injectAnnotations) {
            AnnotationTarget injectTarget = injectAnnotation.target();
            switch (injectAnnotation.target().kind()) {
                case FIELD:
                    injections.add(new Injection(injectTarget, Collections.singletonList(
                            InjectionPointInfo.fromField(injectTarget.asField(), beanClass, beanDeployment, transformer))));
                    break;
                case METHOD:
                    // the spec doesn't forbid generic bean constructors and Weld is fine with them too
                    if (!injectTarget.asMethod().isConstructor()
                            && !injectTarget.asMethod().typeParameters().isEmpty()) {
                        throw new DefinitionException(
                                "Initializer method may not be generic (declare type parameters): " + injectTarget);
                    }

                    injections.add(new Injection(injectTarget,
                            InjectionPointInfo.fromMethod(injectTarget.asMethod(), beanClass, beanDeployment, transformer)));
                    break;
                default:
                    LOGGER.warn("Unsupported @Inject target ignored: " + injectAnnotation.target());
                    continue;
            }
        }
        // if the class has no no-arg constructor and has a single non no-arg constructor that is not annotated with @Inject,
        // the class is not a non-static inner or a superclass of a bean we consider that constructor as an injection
        if (beanClass.equals(classInfo)) {
            final boolean isNonStaticInnerClass = classInfo.name().isInner()
                    && !Modifier.isStatic(classInfo.flags());
            if (!isNonStaticInnerClass && !hasConstructorInjection(injections) && !beanClass.hasNoArgsConstructor()) {
                List<MethodInfo> nonNoargConstrs = new ArrayList<>();
                for (MethodInfo constr : classInfo.methods()) {
                    if (Methods.INIT.equals(constr.name()) && constr.parametersCount() > 0) {
                        nonNoargConstrs.add(constr);
                    }
                }
                if (nonNoargConstrs.size() == 1) {
                    final MethodInfo injectTarget = nonNoargConstrs.get(0);
                    injections.add(new Injection(injectTarget,
                            InjectionPointInfo.fromMethod(injectTarget.asMethod(), beanClass, beanDeployment, transformer)));
                }
            }
        }

        for (DotName resourceAnnotation : beanDeployment.getResourceAnnotations()) {
            List<AnnotationInstance> resourceAnnotations = getAllInjectionPoints(beanDeployment, classInfo,
                    resourceAnnotation, true, seenMethods);
            for (AnnotationInstance resourceAnnotationInstance : resourceAnnotations) {
                if (Kind.FIELD == resourceAnnotationInstance.target().kind()
                        && resourceAnnotationInstance.target().asField().annotations().stream()
                                .noneMatch(a -> DotNames.INJECT.equals(a.name()))) {
                    // Add special injection for a resource field
                    injections.add(new Injection(resourceAnnotationInstance.target(), Collections.singletonList(
                            InjectionPointInfo.fromResourceField(resourceAnnotationInstance.target().asField(),
                                    beanClass, beanDeployment, transformer))));
                }
                // TODO setter injection
            }
        }

        if (!classInfo.superName().equals(DotNames.OBJECT)) {
            ClassInfo info = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (info != null) {
                List<Injection> superInjections = forClassBean(beanClass, info, beanDeployment, transformer, true, seenMethods);

                // injections are discovered bottom-up to easily skip overriden methods,
                // but they need to be performed top-down per the AtInject specification
                superInjections.addAll(injections);
                injections = superInjections;
            }
        }

        return injections;
    }

    static Injection forDisposer(MethodInfo disposerMethod, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        if (beanDeployment.hasAnnotation(disposerMethod, DotNames.INJECT)) {
            throw new DefinitionException("Disposer method must not be annotated @Inject "
                    + "(alternatively, initializer method must not have a @Disposes parameter): "
                    + disposerMethod);
        }
        if (beanDeployment.hasAnnotation(disposerMethod, DotNames.PRODUCES)) {
            throw new DefinitionException("Disposer method must not be annotated @Produces "
                    + "(alternatively, producer method must not have a @Disposes parameter): "
                    + disposerMethod);
        }
        if (Annotations.hasParameterAnnotation(beanDeployment, disposerMethod, DotNames.OBSERVES)) {
            throw new DefinitionException("Disposer method must not have an @Observes parameter "
                    + "(alternatively, observer method must not have a @Disposes parameter): "
                    + disposerMethod);
        }
        if (Annotations.hasParameterAnnotation(beanDeployment, disposerMethod, DotNames.OBSERVES_ASYNC)) {
            throw new DefinitionException("Disposer method must not have an @ObservesAsync parameter "
                    + "(alternatively, async observer method must not have a @Disposes parameter): "
                    + disposerMethod);
        }
        if (Annotations.getParameterAnnotations(beanDeployment, disposerMethod)
                .stream()
                .filter(it -> DotNames.DISPOSES.equals(it.name()))
                .count() > 1) {
            throw new DefinitionException("Disposer method must not have more than 1 @Disposes parameter: "
                    + disposerMethod);
        }

        Injection injection = new Injection(disposerMethod,
                InjectionPointInfo.fromMethod(disposerMethod, beanClass, beanDeployment,
                        (annotations, position) -> annotations.stream().anyMatch(a -> a.name().equals(DotNames.DISPOSES)),
                        transformer));
        injection.injectionPoints.forEach(ipi -> validateInjections(ipi, BeanType.MANAGED_BEAN));
        return injection;
    }

    static Injection forObserver(MethodInfo observerMethod, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        return new Injection(observerMethod, InjectionPointInfo.fromMethod(observerMethod, beanClass, beanDeployment,
                (annotations, position) -> annotations.stream()
                        .anyMatch(a -> a.name().equals(DotNames.OBSERVES) || a.name().equals(DotNames.OBSERVES_ASYNC)),
                transformer));
    }

    static Injection forInvokerArgumentLookups(ClassInfo targetBeanClass, MethodInfo targetMethod,
            boolean[] argumentLookups, BeanDeployment beanDeployment, InjectionPointModifier transformer) {
        return new Injection(targetMethod, InjectionPointInfo.fromMethod(targetMethod, targetBeanClass, beanDeployment,
                (annotations, position) -> !argumentLookups[position], transformer));
    }

    final AnnotationTarget target;

    final List<InjectionPointInfo> injectionPoints;

    public Injection(AnnotationTarget target, List<InjectionPointInfo> injectionPoints) {
        this.target = target;
        this.injectionPoints = injectionPoints;
        if (injectionPoints.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Null injection point detected for " + target);
        }
    }

    boolean isMethod() {
        return target != null && Kind.METHOD == target.kind();
    }

    boolean isConstructor() {
        return isMethod() && target.asMethod().name().equals(Methods.INIT);
    }

    boolean isField() {
        return target != null && Kind.FIELD == target.kind();
    }

    boolean isSynthetic() {
        return target == null;
    }

    /**
     *
     * @return the annotation target or {@code null} in case of synthetic injection
     */
    public AnnotationTarget getTarget() {
        return target;
    }

    public void init(BeanInfo targetBean) {
        for (InjectionPointInfo injectionPoint : injectionPoints) {
            injectionPoint.setTargetBean(targetBean);
        }
    }

    private static boolean hasConstructorInjection(List<Injection> injections) {
        for (Injection injection : injections) {
            if (injection.isConstructor()) {
                return true;
            }
        }
        return false;
    }

    private static List<AnnotationInstance> getAllInjectionPoints(BeanDeployment beanDeployment, ClassInfo beanClass,
            DotName annotationName, boolean skipConstructors, Set<MethodOverrideKey> seenMethods) {
        // order is significant: fields must be injected before methods
        List<AnnotationInstance> injectAnnotations = new ArrayList<>();

        // note that we can't treat static injection as a failure, because
        // that would prevent us from even processing the AtInject TCK;
        // hence, we just print a warning

        for (FieldInfo field : beanClass.fields()) {
            AnnotationInstance inject = beanDeployment.getAnnotation(field, annotationName);
            if (inject != null) {
                if (Modifier.isFinal(field.flags()) || Modifier.isStatic(field.flags())) {
                    LOGGER.warn("An injection field must be non-static and non-final - ignoring: "
                            + field.declaringClass().name() + "#"
                            + field.name());
                } else {
                    injectAnnotations.add(inject);
                }
            }
        }

        for (MethodInfo method : beanClass.methods()) {
            if (skipConstructors && method.name().equals(Methods.INIT)) {
                continue;
            }

            MethodOverrideKey key = new MethodOverrideKey(method);
            if (isOverriden(key, seenMethods)) {
                continue;
            }
            seenMethods.add(key);

            AnnotationInstance inject = beanDeployment.getAnnotation(method, annotationName);
            if (inject != null) {
                if (Modifier.isStatic(method.flags())) {
                    LOGGER.warn("An initializer method must be non-static - ignoring: "
                            + method.declaringClass().name() + "#"
                            + method.name() + "()");
                } else {
                    injectAnnotations.add(inject);
                }
            }
        }

        return injectAnnotations;
    }

    // ---
    // this is close to `Methods.MethodKey` and `Methods.isOverridden()`, but it's actually more precise
    // the `Methods` code is used on many places to avoid processing methods in case a method with the same
    // signature has already been processed, and that's _not_ the definition of overriding

    static class MethodOverrideKey {
        final String name;
        final List<DotName> params;
        final DotName returnType;
        final String visibility;
        final MethodInfo method; // this is intentionally ignored for equals/hashCode

        public MethodOverrideKey(MethodInfo method) {
            this.method = Objects.requireNonNull(method, "Method must not be null");
            this.name = method.name();
            this.returnType = method.returnType().name();
            this.params = new ArrayList<>();
            for (Type i : method.parameterTypes()) {
                params.add(i.name());
            }
            if (Modifier.isPublic(method.flags()) || Modifier.isProtected(method.flags())) {
                this.visibility = "";
            } else if (Modifier.isPrivate(method.flags())) {
                // private methods cannot be overridden
                this.visibility = method.declaringClass().name().toString();
            } else {
                // package-private methods can only be overridden in the same package
                this.visibility = method.declaringClass().name().packagePrefix();
            }
        }

        private MethodOverrideKey(String name, List<DotName> params, DotName returnType, String visibility, MethodInfo method) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.visibility = visibility;
            this.method = method;
        }

        MethodOverrideKey withoutVisibility() {
            return new MethodOverrideKey(name, params, returnType, "", method);
        }

        String packageName() {
            return method.declaringClass().name().packagePrefix();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MethodOverrideKey))
                return false;
            MethodOverrideKey methodKey = (MethodOverrideKey) o;
            return Objects.equals(name, methodKey.name)
                    && Objects.equals(params, methodKey.params)
                    && Objects.equals(returnType, methodKey.returnType)
                    && Objects.equals(visibility, methodKey.visibility);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, params, returnType, visibility);
        }
    }

    /**
     * Returns whether given {@code method} is overridden by any of given {@code previousMethods}. This method
     * only works correctly during a bottom-up traversal of class inheritance hierarchy, during which all seen
     * methods are recorded into {@code previousMethods}.
     * <p>
     * This is not entirely precise according to the JLS rules for method overriding, but seems good enough.
     */
    static boolean isOverriden(MethodOverrideKey method, Set<MethodOverrideKey> previousMethods) {
        short flags = method.method.flags();
        if (Modifier.isPublic(flags) || Modifier.isProtected(flags)) {
            // if there's an override, it must be public or perhaps protected,
            // so it always has the same visibility
            return previousMethods.contains(method);
        } else if (Modifier.isPrivate(flags)) {
            // private methods are never overridden
            return false;
        } else { // package-private
            // if there's an override, it must be in the same package and:
            // 1. either package-private (so it has the same visibility)
            if (previousMethods.contains(method)) {
                return true;
            }

            // 2. or public/protected (so it has a different visibility: empty string)
            String packageName = method.packageName();
            MethodOverrideKey methodWithoutVisibility = method.withoutVisibility();
            for (MethodOverrideKey previousMethod : previousMethods) {
                if (methodWithoutVisibility.equals(previousMethod)
                        && packageName.equals(previousMethod.packageName())) {
                    return true;
                }
            }

            return false;
        }
    }

}
