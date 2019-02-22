package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class TestHttpResourceManager {

    static final String uri;
    static final Map<Class<?>, TestHTTPResourceProvider<?>> providers;

    static {
        Map<Class<?>, TestHTTPResourceProvider<?>> map = new HashMap<>();
        for (TestHTTPResourceProvider i : ServiceLoader.load(TestHTTPResourceProvider.class)) {
            map.put(i.getProvidedType(), i);
        }
        providers = Collections.unmodifiableMap(map);
        Config config = ConfigProvider.getConfig();
        String host = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = config.getOptionalValue("quarkus.http.test-port", String.class).orElse("8081");
        uri = "http://" + host + ":" + port;
        System.setProperty("test.url", uri);
    }

    public static String getUri() {
        return uri;
    }

    public static void inject(Object testCase) {
        Class<?> c = testCase.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                TestHTTPResource resource = f.getAnnotation(TestHTTPResource.class);
                if (resource != null) {
                    TestHTTPResourceProvider provider = providers.get(f.getType());
                    if (provider == null) {
                        throw new RuntimeException(
                                "Unable to inject TestHTTPResource field " + f + " as no provider exists for the type");
                    }
                    String path = resource.value();
                    String val;
                    if (path.startsWith("/")) {
                        val = uri + path;
                    } else {
                        val = uri + "/" + path;
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

}
