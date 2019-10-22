package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class TestHTTPResourceManager {

    public static String getUri() {
        Config config = ConfigProvider.getConfig();
        String host = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = config.getOptionalValue("quarkus.http.test-port", String.class).orElse("8081");
        String contextPath = config.getOptionalValue("quarkus.servlet.context-path", String.class).orElse("");
        return "http://" + host + ":" + port + contextPath;
    }

    public static String getSslUri() {
        Config config = ConfigProvider.getConfig();
        String host = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = config.getOptionalValue("quarkus.http.test-ssl-port", String.class).orElse("8444");
        String contextPath = config.getOptionalValue("quarkus.servlet.context-path", String.class).orElse("");
        return "https://" + host + ":" + port + contextPath;
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
