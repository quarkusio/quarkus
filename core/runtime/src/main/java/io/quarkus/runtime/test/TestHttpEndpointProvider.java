package io.quarkus.runtime.test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * Interface that can be used to integrate with the TestHTTPEndpoint infrastructure
 */
public interface TestHttpEndpointProvider {

    Function<Class<?>, String> endpointProvider();

    static List<Function<Class<?>, String>> load() {
        List<Function<Class<?>, String>> ret = new ArrayList<>();
        try {
            System.out.println("HOLLY loading services, parent is " + TestHttpEndpointProvider.class.getClassLoader());
            System.out.println("HOLLY loading services, will be using is " + Thread.currentThread().getContextClassLoader());
            // TODO what is this doing, is the classloader switch safe?
            // TODO I think the need for the classloader switch shows we accidentally made an extra deployment classloader? ... but doing it this way seems to work
            System.out.println("TCCL's parent is " + Thread.currentThread().getContextClassLoader().getParent());
            // We cannot just pass TestHttpEndpointProvider.class to the service loader because it's loaded with the wrong classloader
            Class<?> serviceClass = Thread.currentThread().getContextClassLoader()
                    .loadClass(TestHttpEndpointProvider.class.getName());
            for (Object i : ServiceLoader.load(serviceClass,
                    Thread.currentThread().getContextClassLoader())) {
                Function<Class<?>, String> thing = (Function<Class<?>, String>) i.getClass().getMethod("endpointProvider")
                        .invoke(i);
                ret.add(thing);
            }
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            // TODO How should we handle the exceptions? Log? Would we even have a logger?
            throw new RuntimeException(e);
        }
        return ret;
    }

}
