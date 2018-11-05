package org.jboss.protean.arc.processor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Vetoed;
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

    private static final Map<String, DotName> NAMES = new ConcurrentHashMap<>();

    static final DotName OBJECT = create(Object.class);
    static final DotName OBSERVES = create(Observes.class);
    static final DotName OBSERVES_ASYNC = create(ObservesAsync.class);
    static final DotName PRODUCES = create(Produces.class);
    static final DotName DISPOSES = create(Disposes.class);
    static final DotName QUALIFIER = create(Qualifier.class);
    static final DotName NONBINDING = create(Nonbinding.class);
    static final DotName INJECT = create(Inject.class);
    static final DotName POST_CONSTRUCT = create(PostConstruct.class);
    static final DotName PRE_DESTROY = create(PreDestroy.class);
    static final DotName INSTANCE = create(Instance.class);
    static final DotName INJECTION_POINT = create(InjectionPoint.class);
    static final DotName INTERCEPTOR = create(Interceptor.class);
    static final DotName INTERCEPTOR_BINDING = create(InterceptorBinding.class);
    static final DotName AROUND_INVOKE = create(AroundInvoke.class);
    static final DotName AROUND_CONSTRUCT = create(AroundConstruct.class);
    static final DotName PRIORITY = create(Priority.class);
    static final DotName DEFAULT = create(Default.class);
    static final DotName ANY = create(Any.class);
    static final DotName BEAN = create(Bean.class);
    static final DotName BEAN_MANAGER = create(BeanManager.class);
    static final DotName EVENT = create(Event.class);
    static final DotName EVENT_METADATA = create(EventMetadata.class);
    static final DotName ALTERNATIVE = create(Alternative.class);
    static final DotName STEREOTYPE = create(Stereotype.class);
    static final DotName TYPED = create(Typed.class);
    static final DotName VETOED = create(Vetoed.class);
    static final DotName CLASS = create(Class.class);
    static final DotName ENUM = create(Enum.class);

    private DotNames() {
    }

    static DotName create(Class<?> clazz) {
        return create(clazz.getName());
    }

    static DotName create(String name) {
        if (!name.contains(".")) {
            return DotName.createComponentized(null, name);
        }
        String prefix = name.substring(0, name.lastIndexOf('.'));
        DotName prefixName = NAMES.computeIfAbsent(prefix, DotNames::create);
        String local = name.substring(name.lastIndexOf('.') + 1);
        return DotName.createComponentized(prefixName, local);
    }

    static String simpleName(DotName dotName) {
        String local = dotName.local();
        return local.contains(".") ? Types.convertNested(local.substring(local.lastIndexOf("."), local.length())) : Types.convertNested(local);
    }

    static String packageName(DotName dotName) {
        String name = dotName.toString();
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return name.substring(0, index);
    }

}
