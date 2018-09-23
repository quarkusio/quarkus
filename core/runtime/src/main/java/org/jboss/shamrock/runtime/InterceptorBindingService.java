package org.jboss.shamrock.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.interceptor.InvocationContext;

/**
 * This is a service that allows an app to get interceptor bindings from the {@link javax.interceptor.InvocationContext}
 * <p>
 * It is in core so that no direct dep is needed on either arc or weld, as they both do this differently
 * <p>
 * TODO: this should really be moved to some kind of CDI common module or something, also this should probably be temporary
 */
public class InterceptorBindingService {


    public static List<Provider> providers;

    static {
        List<Provider> a = new ArrayList<>();
        for (Provider i : ServiceLoader.load(Provider.class)) {
            a.add(i);
        }
        providers = Collections.unmodifiableList(a);
    }

    public static Set<Annotation> getInterceptorBindings(InvocationContext ic) {
        for (Provider i : providers) {
            Set<Annotation> ret = i.getInterceptorBindings(ic);
            if (ret != null) {
                return ret;
            }
        }
        return Collections.emptySet();
    }


    public interface Provider {

        Set<Annotation> getInterceptorBindings(InvocationContext invocationContext);

    }

}
