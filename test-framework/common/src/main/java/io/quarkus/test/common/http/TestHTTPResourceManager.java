package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.ConfigProvider;

public class TestHTTPResourceManager {

    public static String getUri() {
        try {
            return ConfigProvider.getConfig().getValue("test.url", String.class);
        } catch (IllegalStateException e) {
            //massive hack for dev mode tests, dev mode has not started yet
            //so we don't have any way to load this correctly from config
            return "http://localhost:8080";
        }
    }

    public static String getSslUri() {
        return ConfigProvider.getConfig().getValue("test.url.ssl", String.class);
    }

    public static void inject(Object testCase) {
        Map<Class<?>, TestHTTPResourceProvider<?>> providers = getProviders();
        Class<?> c = testCase.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                TestHTTPResource resource = f.getAnnotation(TestHTTPResource.class);
                if (resource != null) {
                    TestHTTPResourceProvider<?> provider = providers.get(f.getType());
                    if (provider == null) {
                        throw new RuntimeException(
                                "Unable to inject TestHTTPResource field " + f + " as no provider exists for the type");
                    }
                    String path = resource.value();
                    String val;
                    if (resource.ssl()) {
                        if (path.startsWith("/")) {
                            val = getSslUri() + path;
                        } else {
                            val = getSslUri() + "/" + path;
                        }
                    } else {
                        if (path.startsWith("/")) {
                            val = getUri() + path;
                        } else {
                            val = getUri() + "/" + path;
                        }
                    }
                    f.setAccessible(true);
                    try {
                        f.set(testCase, provider.provide(new URI(val), f));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private static Map<Class<?>, TestHTTPResourceProvider<?>> getProviders() {
        Map<Class<?>, TestHTTPResourceProvider<?>> map = new HashMap<>();
        for (TestHTTPResourceProvider<?> i : ServiceLoader.load(TestHTTPResourceProvider.class)) {
            map.put(i.getProvidedType(), i);
        }
        return Collections.unmodifiableMap(map);
    }
}
