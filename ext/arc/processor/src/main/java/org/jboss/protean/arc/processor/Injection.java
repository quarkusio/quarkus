package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * Injection abstraction - an injected field, a bean constructor, an initializer or a disposer method.
 *
 * @author Martin Kouba
 */
public class Injection {

    private static final Logger LOGGER = Logger.getLogger(Injection.class);

    private static final DotName JAVA_LANG_OBJECT = DotName.createSimple(Object.class.getName());
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
                        injections.add(new Injection(injectTarget,
                                Collections.singletonList(InjectionPointInfo.fromField(injectTarget.asField(), beanDeployment))));
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
        if(!beanTarget.superName().equals(JAVA_LANG_OBJECT)) {
            ClassInfo info = beanDeployment.getIndex().getClassByName(beanTarget.superName());
            if(info != null) {
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
                annotations -> annotations.stream().anyMatch(a -> a.name().equals(DotNames.OBSERVES))));
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
