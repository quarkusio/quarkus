package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.DefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

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

    /**
     *
     * @param beanTarget
     * @param beanDeployment
     * @return the list of injections
     */
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
            return injections;
        } else if (Kind.METHOD.equals(beanTarget.kind())) {
            if (beanTarget.asMethod().parameters().isEmpty()) {
                return Collections.emptyList();
            }
            // All parameters are injection points
            return Collections.singletonList(
                    new Injection(beanTarget.asMethod(),
                            InjectionPointInfo.fromMethod(beanTarget.asMethod(), declaringBean.getImplClazz(),
                                    beanDeployment, transformer)));
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
                    injections
                            .add(new Injection(injectTarget, Collections
                                    .singletonList(
                                            InjectionPointInfo.fromField(injectTarget.asField(), beanClass, beanDeployment,
                                                    transformer))));
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
                    if (Methods.INIT.equals(constr.name()) && constr.parameters().size() > 0) {
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
                    injections.add(new Injection(resourceAnnotationInstance.target(), Collections
                            .singletonList(InjectionPointInfo
                                    .fromResourceField(resourceAnnotationInstance.target().asField(), beanClass,
                                            beanDeployment, transformer))));
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

    private static boolean hasConstructorInjection(List<Injection> injections) {
        for (Injection injection : injections) {
            if (injection.isConstructor()) {
                return true;
            }
        }
        return false;
    }

    static Injection forDisposer(MethodInfo disposerMethod, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
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
        return Kind.METHOD == target.kind();
    }

    boolean isConstructor() {
        return isMethod() && target.asMethod().name().equals(Methods.INIT);
    }

    boolean isField() {
        return Kind.FIELD == target.kind();
    }

    public AnnotationTarget getTarget() {
        return target;
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
