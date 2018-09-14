package org.jboss.protean.arc.processor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;

import org.jboss.jandex.DotName;

final class DotNames {

    static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    static final DotName OBSERVES = DotName.createSimple(Observes.class.getName());
    static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());
    static final DotName DISPOSES = DotName.createSimple(Disposes.class.getName());
    static final DotName QUALIFIER = DotName.createSimple(Qualifier.class.getName());
    static final DotName NONBINDING = DotName.createSimple(Nonbinding.class.getName());
    static final DotName INJECT = DotName.createSimple(Inject.class.getName());
    static final DotName POST_CONSTRUCT = DotName.createSimple(PostConstruct.class.getName());
    static final DotName PRE_DESTROY = DotName.createSimple(PreDestroy.class.getName());
    static final DotName INSTANCE = DotName.createSimple(Instance.class.getName());
    static final DotName INJECTION_POINT = DotName.createSimple(InjectionPoint.class.getName());
    static final DotName INTERCEPTOR = DotName.createSimple(Interceptor.class.getName());
    static final DotName INTERCEPTOR_BINDING = DotName.createSimple(InterceptorBinding.class.getName());
    static final DotName AROUND_INVOKE = DotName.createSimple(AroundInvoke.class.getName());
    static final DotName AROUND_CONSTRUCT = DotName.createSimple(AroundConstruct.class.getName());
    static final DotName PRIORITY = DotName.createSimple(Priority.class.getName());
    static final DotName DEFAULT = DotName.createSimple(Default.class.getName());
    static final DotName ANY = DotName.createSimple(Any.class.getName());
    static final DotName BEAN = DotName.createSimple(Bean.class.getName());
    static final DotName BEAN_MANAGER = DotName.createSimple(BeanManager.class.getName());
    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName EVENT_METADATA = DotName.createSimple(EventMetadata.class.getName());
    static final DotName ALTERNATIVE = DotName.createSimple(Alternative.class.getName());
    static final DotName STEREOTYPE = DotName.createSimple(Stereotype.class.getName());
    static final DotName TYPED = DotName.createSimple(Typed.class.getName());

    private DotNames() {
    }

    static String simpleName(DotName dotName) {
        String local = dotName.local();
        return local.contains(".") ? Types.convertNested(local.substring(local.lastIndexOf("."), local.length())) : Types.convertNested(local);
    }

    static String packageName(DotName dotName) {
        String name = dotName.toString();
        return name.contains(".") ? name.substring(0, name.lastIndexOf(".")) : "";
    }

}
