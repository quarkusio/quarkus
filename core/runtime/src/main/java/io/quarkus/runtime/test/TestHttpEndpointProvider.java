package io.quarkus.runtime.test;

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
        for (TestHttpEndpointProvider i : ServiceLoader.load(TestHttpEndpointProvider.class,
                Thread.currentThread().getContextClassLoader())) {
            ret.add(i.endpointProvider());
        }
        return ret;
    }

}
