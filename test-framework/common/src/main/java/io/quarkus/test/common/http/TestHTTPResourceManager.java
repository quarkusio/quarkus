package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.runtime.test.TestHttpEndpointProvider;

public class TestHTTPResourceManager {

    public static String getUri() {
        try {
            return ConfigProvider.getConfig().getValue("test.url", String.class);
        } catch (IllegalStateException e) {
            // massive hack for dev mode tests, dev mode has not started yet
            // so we don't have any way to load this correctly from config
            return "http://localhost:8080";
        }
    }

    public static String getManagementUri() {
        try {
            return ConfigProvider.getConfig().getValue("test.management.url", String.class);
        } catch (IllegalStateException e) {
            // massive hack for dev mode tests, dev mode has not started yet
            // so we don't have any way to load this correctly from config
            return "http://localhost:9000";
        }
    }

    public static String getSslUri() {
        return ConfigProvider.getConfig().getValue("test.url.ssl", String.class);
    }

    public static String getManagementSslUri() {
        return ConfigProvider.getConfig().getValue("test.management.url.ssl", String.class);
    }

    public static String getUri(RunningQuarkusApplication application) {
        return application.getConfigValue("test.url", String.class).get();
    }

    public static String getSslUri(RunningQuarkusApplication application) {
        return application.getConfigValue("test.url.ssl", String.class).get();
    }

    public static void inject(Object testCase) {
        inject(testCase, TestHttpEndpointProvider.load());
    }

    public static void inject(Object testCase, List<Function<Class<?>, String>> endpointProviders) {
        Map<Class<?>, TestHTTPResourceProvider<?>> providers = getProviders();
        Class<?> c = testCase.getClass();
        while (c != Object.class) {
            TestHTTPEndpoint classEndpointAnnotation = c.getAnnotation(TestHTTPEndpoint.class);
            for (Field f : c.getDeclaredFields()) {
                TestHTTPResource resource = f.getAnnotation(TestHTTPResource.class);
                if (resource != null) {
                    TestHTTPResourceProvider<?> provider = providers.get(f.getType());
                    if (provider == null) {
                        throw new RuntimeException(
                                "Unable to inject TestHTTPResource field " + f + " as no provider exists for the type");
                    }
                    String path = resource.value();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    String endpointPath = null;
                    boolean management = resource.management();
                    TestHTTPEndpoint fieldEndpointAnnotation = f.getAnnotation(TestHTTPEndpoint.class);
                    if (fieldEndpointAnnotation != null) {
                        endpointPath = getEndpointPath(endpointProviders, f, fieldEndpointAnnotation);
                    } else if (classEndpointAnnotation != null) {
                        endpointPath = getEndpointPath(endpointProviders, f, classEndpointAnnotation);
                    }
                    if (!path.isEmpty() && endpointPath != null) {
                        if (endpointPath.endsWith("/")) {
                            path = endpointPath + path;
                        } else {
                            path = endpointPath + "/" + path;
                        }
                    } else if (endpointPath != null) {
                        path = endpointPath;
                    }
                    String val;
                    if (resource.ssl() || resource.tls() || ConfigProvider.getConfig()
                            .getOptionalValue("quarkus.http.test-ssl-enabled", Boolean.class).orElse(false)) {
                        if (management) {
                            if (path.startsWith("/")) {
                                val = getManagementSslUri() + path;
                            } else {
                                val = getManagementSslUri() + "/" + path;
                            }
                        } else {
                            if (path.startsWith("/")) {
                                val = getSslUri() + path;
                            } else {
                                val = getSslUri() + "/" + path;
                            }
                        }
                    } else {
                        if (management) {
                            if (path.startsWith("/")) {
                                val = getManagementUri() + path;
                            } else {
                                val = getManagementUri() + "/" + path;
                            }
                        } else {
                            if (path.startsWith("/")) {
                                val = getUri() + path;
                            } else {
                                val = getUri() + "/" + path;
                            }
                        }
                    }
                    f.setAccessible(true);
                    try {
                        f.set(testCase, provider.provide(val, f));
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
        for (TestHTTPResourceProvider<?> i : ServiceLoader.load(TestHTTPResourceProvider.class,
                TestHTTPResourceProvider.class.getClassLoader())) {
            map.put(i.getProvidedType(), i);
        }
        return Collections.unmodifiableMap(map);
    }

    private static String getEndpointPath(List<Function<Class<?>, String>> endpointProviders, Field field,
            TestHTTPEndpoint endpointAnnotation) {
        for (Function<Class<?>, String> func : endpointProviders) {
            String endpointPath = func.apply(endpointAnnotation.value());
            if (endpointPath != null) {
                return endpointPath;
            }
        }
        throw new RuntimeException(
                "Could not determine the endpoint path for " + endpointAnnotation.value()
                        + " to inject " + field);
    }

}
