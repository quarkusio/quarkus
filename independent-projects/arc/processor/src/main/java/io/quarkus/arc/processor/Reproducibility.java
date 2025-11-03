package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class Reproducibility {
    static final Comparator<BeanInfo> BEAN_COMPARATOR = Comparator.comparing(BeanInfo::getIdentifier);
    static final Comparator<ObserverInfo> OBSERVER_COMPARATOR = Comparator.comparing(ObserverInfo::getIdentifier);

    static List<BeanInfo> orderedBeans(Collection<BeanInfo> beans) {
        List<BeanInfo> list = new ArrayList<>(beans);
        list.sort(BEAN_COMPARATOR);
        return list;
    }

    static List<InterceptorInfo> orderedInterceptors(Collection<InterceptorInfo> interceptors) {
        List<InterceptorInfo> list = new ArrayList<>(interceptors);
        list.sort(BEAN_COMPARATOR);
        return list;
    }

    static List<DecoratorInfo> orderedDecorators(Collection<DecoratorInfo> decorators) {
        List<DecoratorInfo> list = new ArrayList<>(decorators);
        list.sort(BEAN_COMPARATOR);
        return list;
    }

    static List<ObserverInfo> orderedObservers(Collection<ObserverInfo> observers) {
        List<ObserverInfo> list = new ArrayList<>(observers);
        list.sort(OBSERVER_COMPARATOR);
        return list;
    }
}
