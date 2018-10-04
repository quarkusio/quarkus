package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
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
    static List<Injection> forBean(AnnotationTarget beanTarget, BeanDeployment beanDeployment) {
        if (Kind.CLASS.equals(beanTarget.kind())) {
            List<Injection> injections = new ArrayList<>();
            forClassBean(beanTarget.asClass(), beanDeployment, injections);
            return injections;
        } else if (Kind.METHOD.equals(beanTarget.kind())) {
            if (beanTarget.asMethod().parameters().isEmpty()) {
                return Collections.emptyList();
            }
            // All parameters are injection points
            return Collections.singletonList(new Injection(beanTarget.asMethod(), InjectionPointInfo.fromMethod(beanTarget.asMethod(), beanDeployment)));
        }
        throw new IllegalArgumentException("Unsupported annotation target");
    }

    private static void forClassBean(ClassInfo beanTarget, BeanDeployment beanDeployment, List<Injection> injections) {
        List<AnnotationInstance> injectAnnotations = beanTarget.annotations().get(DotNames.INJECT);
        if (injectAnnotations != null) {
            for (AnnotationInstance injectAnnotation : injectAnnotations) {
                AnnotationTarget injectTarget = injectAnnotation.target();
                switch (injectAnnotation.target().kind()) {
                    case FIELD:
                        injections.add(
                                new Injection(injectTarget, Collections.singletonList(InjectionPointInfo.fromField(injectTarget.asField(), beanDeployment))));
                        break;
                    case METHOD:
                        injections.add(new Injection(injectTarget, InjectionPointInfo.fromMethod(injectTarget.asMethod(), beanDeployment)));
                        break;
                    default:
                        LOGGER.warn("Unsupported @Inject target ignored: " + injectAnnotation.target());
                        continue;
                }
            }
        }
        if (!beanTarget.superName().equals(DotNames.OBJECT)) {
            ClassInfo info = beanDeployment.getIndex().getClassByName(beanTarget.superName());
            if (info != null) {
                forClassBean(info, beanDeployment, injections);
            }
        }

    }

    static Injection forDisposer(MethodInfo disposerMethod, BeanDeployment beanDeployment) {
        return new Injection(disposerMethod, InjectionPointInfo.fromMethod(disposerMethod, beanDeployment,
                annotations -> annotations.stream().anyMatch(a -> a.name().equals(DotNames.DISPOSES))));
    }

    static Injection forObserver(MethodInfo observerMethod, BeanDeployment beanDeployment) {
        return new Injection(observerMethod, InjectionPointInfo.fromMethod(observerMethod, beanDeployment,
                annotations -> annotations.stream().anyMatch(a -> a.name().equals(DotNames.OBSERVES) || a.name().equals(DotNames.OBSERVES_ASYNC))));
    }

    final AnnotationTarget target;

    final List<InjectionPointInfo> injectionPoints;

    public Injection(AnnotationTarget target, List<InjectionPointInfo> injectionPoints) {
        this.target = target;
        this.injectionPoints = injectionPoints;
    }

    boolean isMethod() {
        return Kind.METHOD.equals(target.kind());
    }

    boolean isConstructor() {
        return isMethod() && target.asMethod().name().equals("<init>");
    }

    boolean isField() {
        return Kind.FIELD.equals(target.kind());
    }

}
