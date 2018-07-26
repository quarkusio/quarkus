package org.jboss.shamrock.deployment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.inject.Inject;

public class DeploymentProcessorInjection {

    private final List<InjectionProvider> injectionProviders = new ArrayList<>();

    private Map<Class<?>, InjectionProvider> providedTypes = new HashMap<>();

    DeploymentProcessorInjection(ClassLoader loader) {
        Iterator<InjectionProvider> ip = ServiceLoader.load(InjectionProvider.class, loader).iterator();
        while (ip.hasNext()) {
            InjectionProvider next = ip.next();
            Set<Class<?>> pro = next.getProvidedTypes();
            for (Class<?> i : pro) {
                if (providedTypes.containsKey(i)) {
                    throw new RuntimeException("Multiple injection providers provide " + i + " provided by " + next + " and " + providedTypes.get(i));
                }
                providedTypes.put(i, next);
            }

            injectionProviders.add(next);
        }
    }

    void injectClass(Object instance) {
        injectClass(instance, instance.getClass());
    }

    private void injectClass(Object instance, Class<?> theClass) {
        if (theClass == Object.class) {
            return;
        }
        try {
            for (Field f : theClass.getDeclaredFields()) {
                if (f.getAnnotation(Inject.class) != null) {
                    InjectionProvider provider = providedTypes.get(f.getType());
                    if (provider == null) {
                        throw new RuntimeException("No injection provider able to inject field " + f);
                    }
                    f.setAccessible(true);
                    f.set(instance, provider.getInstance(f.getType()));
                }
            }
            injectClass(instance, theClass.getSuperclass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
