package io.quarkus.runtime.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

        // TODO sometimes - always - this is loaded with the system classloader, but if we can fix that, we can drop the reflection
        // TODO why does this have to be different than it was in the pre-WG-30 world?
        // TODO does fixing the loading of QTE with the system classloader fix this?
        // TODO add a bypass path if the classloader is the same? - not worth it, it never is
        // TODO #store

        List<Function<Class<?>, String>> ret = new ArrayList<>();
        System.out.println("HOLLY wull load " + TestHttpEndpointProvider.class.getClassLoader() + " and tccl "
                + Thread.currentThread().getContextClassLoader());
        try {

            ClassLoader targetclassloader = Thread.currentThread().getContextClassLoader(); // TODO why did I have this as TestHttpEndpointProvider.class.getClassLoader(); //
            Class target = targetclassloader.loadClass(TestHttpEndpointProvider.class.getName());
            Method method = target.getMethod("endpointProvider");

            for (Object i : ServiceLoader.load(target,
                    targetclassloader)) {
                ret.add((Function<Class<?>, String>) method.invoke(i));

            }
        } catch (IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

}
