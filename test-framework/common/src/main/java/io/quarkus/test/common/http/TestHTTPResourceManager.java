package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.ListeningAddress;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.SmallRyeConfig;

public class TestHTTPResourceManager {

    @Deprecated(forRemoval = true, since = "3.31")
    public static String getUri() {
        try {
            return ConfigProvider.getConfig().getValue("test.url", String.class);
        } catch (IllegalStateException e) {
            // massive hack for dev mode tests, dev mode has not started yet
            // so we don't have any way to load this correctly from config
            return "http://localhost:8080";
        }
    }

    @Deprecated(forRemoval = true, since = "3.31")
    public static String getManagementUri() {
        try {
            return ConfigProvider.getConfig().getValue("test.management.url", String.class);
        } catch (IllegalStateException e) {
            // massive hack for dev mode tests, dev mode has not started yet
            // so we don't have any way to load this correctly from config
            return "http://localhost:9000";
        }
    }

    @Deprecated(forRemoval = true, since = "3.31")
    public static String getSslUri() {
        return ConfigProvider.getConfig().getValue("test.url.ssl", String.class);
    }

    @Deprecated(forRemoval = true, since = "3.31")
    public static String getManagementSslUri() {
        return ConfigProvider.getConfig().getValue("test.management.url.ssl", String.class);
    }

    @Deprecated(forRemoval = true, since = "3.31")
    public static String getUri(RunningQuarkusApplication application) {
        return application.getConfigValue("test.url", String.class).get();
    }

    @Deprecated(forRemoval = true, since = "3.31")
    public static String getSslUri(RunningQuarkusApplication application) {
        return application.getConfigValue("test.url.ssl", String.class).get();
    }

    public static void inject(Object testCase, ValueRegistry valueRegistry) {
        inject(testCase, valueRegistry, TestHttpEndpointProvider.load());
    }

    public static void inject(
            Object testCase,
            ValueRegistry valueRegistry,
            List<Function<Class<?>, String>> endpointProviders) {

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
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
                    if (resource.tls()
                            || config.getOptionalValue("quarkus.http.test-ssl-enabled", Boolean.class).orElse(false)) {
                        if (management) {
                            val = testManagementUrlSsl(valueRegistry, config, path);
                        } else {
                            val = testUrlSsl(valueRegistry, config, path);
                        }
                    } else {
                        if (management) {
                            val = testManagementUrl(valueRegistry, config, path);
                        } else {
                            val = testUrl(valueRegistry, config, path);
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

    public static String testUrl(ValueRegistry valueRegistry, SmallRyeConfig config, String... paths) {
        String host = host(config, "quarkus.http.host");
        int port = valueRegistry.getOrDefault(ListeningAddress.HTTP_TEST_PORT, 8081);
        String rootPath = rootPath(config, paths);
        return "http://" + host + ":" + port + rootPath;
    }

    public static String testManagementUrl(ValueRegistry valueRegistry, SmallRyeConfig config, String... paths) {
        String host = host(config, "quarkus.management.host");
        int port = valueRegistry.getOrDefault(RuntimeKey.intKey("quarkus.management.test-port"), 9001);
        String managementRootPath = managementRootPath(config, paths);
        return "http://" + host + ":" + port + managementRootPath;
    }

    public static String testUrlSsl(ValueRegistry valueRegistry, SmallRyeConfig config, String... paths) {
        String host = host(config, "quarkus.http.host");
        int port = valueRegistry.getOrDefault(ListeningAddress.HTTPS_TEST_PORT, 8444);
        String rootPath = rootPath(config, paths);
        return "https://" + host + ":" + port + rootPath;
    }

    public static String testManagementUrlSsl(ValueRegistry valueRegistry, SmallRyeConfig config, String... paths) {
        String host = host(config, "quarkus.management.host");
        int port = valueRegistry.getOrDefault(RuntimeKey.intKey("quarkus.management.test-port"), 9001);
        String managementRootPath = managementRootPath(config, paths);
        return "https://" + host + ":" + port + managementRootPath;
    }

    public static String host(SmallRyeConfig config, String name) {
        String host = config.getOptionalValue(name, String.class).orElse("localhost");
        // for test, the host default is localhost, but if using WSL is 0.0.0.0 which shouldn't be used when determining the test url
        if (host.equals("0.0.0.0")) {
            host = "localhost";
        }
        return host;
    }

    public static String rootPath(SmallRyeConfig config, String... paths) {
        String rootPath = config.getOptionalValue("quarkus.http.root-path", String.class).orElse("/");
        Optional<String> contextPath = config.getOptionalValue("quarkus.servlet.context-path", String.class);
        StringBuilder path = new StringBuilder(rootPath);
        if (!rootPath.startsWith("/")) {
            path.insert(0, "/");
        }
        if (!rootPath.endsWith("/")) {
            path.append("/");
        }
        if (contextPath.isPresent()) {
            String relativePath = contextPath.get().startsWith("/") ? contextPath.get().substring(1) : contextPath.get();
            path.append(relativePath);
            if (!relativePath.endsWith("/")) {
                path.append("/");
            }
        }
        for (String p : paths) {
            String relativePath = p.startsWith("/") ? p.substring(1) : p;
            path.append(relativePath);
        }
        return path.toString();
    }

    public static String managementRootPath(SmallRyeConfig config, String... paths) {
        String rootPath = config.getOptionalValue("quarkus.management.root-path", String.class).orElse("/q");
        StringBuilder path = new StringBuilder(rootPath);
        if (!rootPath.startsWith("/")) {
            path.insert(0, "/");
        }
        if (!rootPath.endsWith("/")) {
            path.append("/");
        }
        for (String p : paths) {
            String relativePath = p.startsWith("/") ? p.substring(1) : p;
            path.append(relativePath);
        }
        return path.toString();
    }
}
