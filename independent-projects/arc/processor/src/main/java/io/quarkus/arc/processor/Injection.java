package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * <li>an observer method.</li>
 * </ul>
 *
 * @author Martin Kouba
 */
public class Injection {

    private static final Logger LOGGER = Logger.getLogger(Injection.class);

    static Injection forSyntheticBean(Iterable<TypeAndQualifiers> injectionPoints) {
        List<InjectionPointInfo> ips = new ArrayList<>();
        for (TypeAndQualifiers injectionPoint : injectionPoints) {
            ips.add(InjectionPointInfo.fromSyntheticInjectionPoint(injectionPoint));
        }
        return new Injection(null, ips);
    }

    static List<Injection> forBean(AnnotationTarget beanTarget, BeanInfo declaringBean, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        if (Kind.CLASS.equals(beanTarget.kind())) {
            List<Injection> injections = new ArrayList<>();
            forClassBean(beanTarget.asClass(), beanTarget.asClass(), beanDeployment, injections, transformer, false);

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
            return Collections.singletonList(new Injection(producerMethod,
                    InjectionPointInfo.fromMethod(producerMethod, declaringBean.getImplClazz(), beanDeployment, transformer)));
        }
        throw new IllegalArgumentException("Unsupported annotation target");
    }

    private static void forClassBean(ClassInfo beanClass, ClassInfo classInfo, BeanDeployment beanDeployment,
            List<Injection> injections, InjectionPointModifier transformer, boolean skipConstructors) {

        List<AnnotationInstance> injectAnnotations = getAllInjectionPoints(beanDeployment, classInfo, DotNames.INJECT,
                skipConstructors);

        for (AnnotationInstance injectAnnotation : injectAnnotations) {
            AnnotationTarget injectTarget = injectAnnotation.target();
            switch (injectAnnotation.target().kind()) {
                case FIELD:
                    injections.add(new Injection(injectTarget, Collections.singletonList(
                            InjectionPointInfo.fromField(injectTarget.asField(), beanClass, beanDeployment, transformer))));
                    break;
                case METHOD:
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
                    resourceAnnotation, true);
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
                forClassBean(beanClass, info, beanDeployment, injections, transformer, true);
            }
        }

    }

    static Injection forDisposer(MethodInfo disposerMethod, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer, BeanInfo declaringBean) {
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

        return new Injection(disposerMethod, InjectionPointInfo.fromMethod(disposerMethod, beanClass, beanDeployment,
                annotations -> annotations.stream().anyMatch(a -> a.name().equals(DotNames.DISPOSES)), transformer));
    }

    static Injection forObserver(MethodInfo observerMethod, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        return new Injection(observerMethod, InjectionPointInfo.fromMethod(observerMethod, beanClass, beanDeployment,
                annotations -> annotations.stream()
                        .anyMatch(a -> a.name().equals(DotNames.OBSERVES) || a.name().equals(DotNames.OBSERVES_ASYNC)),
                transformer));
    }

    final AnnotationTarget target;

    final List<InjectionPointInfo> injectionPoints;

    public Injection(AnnotationTarget target, List<InjectionPointInfo> injectionPoints) {
        this.target = target;
        this.injectionPoints = injectionPoints;
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
            DotName name, boolean skipConstructors) {
        List<AnnotationInstance> injectAnnotations = new ArrayList<>();
        for (FieldInfo field : beanClass.fields()) {
            AnnotationInstance inject = beanDeployment.getAnnotation(field, name);
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
            AnnotationInstance inject = beanDeployment.getAnnotation(method, name);
            if (inject != null) {
                injectAnnotations.add(inject);
            }
        }
        return injectAnnotations;
    }

}
