package io.quarkus.test.common.http;

import static io.quarkus.test.common.ListeningAddress.LOCAL_BASE_URI;
import static io.quarkus.test.common.ListeningAddress.LOCAL_MANAGEMENT_BASE_URI;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.ListeningAddress;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfig;

public class TestHTTPResourceManager {

    private static final RuntimeKey<URI> LAMBDA_BASE_URI = RuntimeKey.key("quarkus.lambda.local-base-uri");

    public static void inject(Object testCase, ValueRegistry valueRegistry) {
        inject(testCase, valueRegistry, Config.get(), TestHttpEndpointProvider.load());
    }

    public static void inject(Object testCase, ValueRegistry valueRegistry, Config config) {
        inject(testCase, valueRegistry, config, TestHttpEndpointProvider.load());
    }

    public static void inject(
            Object testCase,
            ValueRegistry valueRegistry,
            Config config,
            List<Function<Class<?>, String>> endpointProviders) {

        Map<Class<?>, TestHTTPResourceProvider<?>> providers = null;
        Class<?> c = testCase.getClass();
        while (c != Object.class) {
            TestHTTPEndpoint classEndpointAnnotation = c.getAnnotation(TestHTTPEndpoint.class);
            for (Field f : c.getDeclaredFields()) {
                TestHTTPResource resource = f.getAnnotation(TestHTTPResource.class);
                if (resource != null) {
                    if (config == null) {
                        config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                    }
                    if (providers == null) {
                        providers = getProviders();
                    }
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

    public static String testUrl(ValueRegistry valueRegistry, Config config, String... paths) {
        // Check if Lambda base URI is registered (for Lambda tests)
        if (valueRegistry.containsKey(LAMBDA_BASE_URI)) {
            return appendPaths(valueRegistry.get(LAMBDA_BASE_URI), paths);
        }
        // Check if local base URI is registered (for regular HTTP tests)
        if (valueRegistry.containsKey(LOCAL_BASE_URI)) {
            return appendPaths(valueRegistry.get(LOCAL_BASE_URI), paths);
        }
        // Fall back to constructing URL from config (for compatibility)
        String host = host(config, "quarkus.http.host");
        int port = valueRegistry.getOrDefault(ListeningAddress.HTTP_TEST_PORT, 8081);
        String rootPath = rootPath(config, paths);
        return "http://" + host + ":" + port + rootPath;
    }

    public static String testManagementUrl(ValueRegistry valueRegistry, Config config, String... paths) {
        // Check if local management base URI is registered
        if (valueRegistry.containsKey(LOCAL_MANAGEMENT_BASE_URI)) {
            return appendPaths(valueRegistry.get(LOCAL_MANAGEMENT_BASE_URI), paths);
        }
        // Fall back to constructing URL from config (for compatibility)
        String host = host(config, "quarkus.management.host");
        int port = valueRegistry.getOrDefault(ListeningAddress.MANAGEMENT_TEST_PORT, 9001);
        String managementRootPath = managementRootPath(config, paths);
        return "http://" + host + ":" + port + managementRootPath;
    }

    public static String testUrlSsl(ValueRegistry valueRegistry, Config config, String... paths) {
        // Check if Lambda base URI is registered (for Lambda tests)
        if (valueRegistry.containsKey(LAMBDA_BASE_URI)) {
            URI baseUri = valueRegistry.get(LAMBDA_BASE_URI);
            // Use https if the base URI is https, otherwise use the registered base as-is
            if ("https".equals(baseUri.getScheme())) {
                return appendPaths(baseUri, paths);
            }
        }
        // Check if local base URI is registered (for regular HTTP tests)
        if (valueRegistry.containsKey(LOCAL_BASE_URI)) {
            URI baseUri = valueRegistry.get(LOCAL_BASE_URI);
            // Use https if the base URI is https, otherwise use the registered base as-is
            if ("https".equals(baseUri.getScheme())) {
                return appendPaths(baseUri, paths);
            }
        }
        // Fall back to constructing URL from config (for compatibility)
        String host = host(config, "quarkus.http.host");
        int port = valueRegistry.getOrDefault(ListeningAddress.HTTPS_TEST_PORT, 8444);
        String rootPath = rootPath(config, paths);
        return "https://" + host + ":" + port + rootPath;
    }

    public static String testManagementUrlSsl(ValueRegistry valueRegistry, Config config, String... paths) {
        // Check if local management base URI is registered
        if (valueRegistry.containsKey(LOCAL_MANAGEMENT_BASE_URI)) {
            URI baseUri = valueRegistry.get(LOCAL_MANAGEMENT_BASE_URI);
            // Use https if the base URI is https, otherwise use the registered base as-is
            if ("https".equals(baseUri.getScheme())) {
                return appendPaths(baseUri, paths);
            }
        }
        // Fall back to constructing URL from config (for compatibility)
        String host = host(config, "quarkus.management.host");
        int port = valueRegistry.getOrDefault(ListeningAddress.MANAGEMENT_TEST_PORT, 9001);
        String managementRootPath = managementRootPath(config, paths);
        return "https://" + host + ":" + port + managementRootPath;
    }

    public static String host(Config config, String name) {
        String host = config.getOptionalValue(name, String.class).orElse("localhost");
        // for test, the host default is localhost, but if using WSL is 0.0.0.0 which shouldn't be used when determining the test url
        if (host.equals("0.0.0.0")) {
            host = "localhost";
        }
        return host;
    }

    public static String rootPath(Config config, String... paths) {
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

    public static String managementRootPath(Config config, String... paths) {
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

    private static String appendPaths(URI baseUri, String... paths) {
        String baseUrl = baseUri.toString();
        if (paths.length == 0) {
            return baseUrl;
        }
        StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/")) {
            url.append("/");
        }
        for (String path : paths) {
            if (path != null && !path.isEmpty()) {
                String relativePath = path.startsWith("/") ? path.substring(1) : path;
                url.append(relativePath);
            }
        }
        return url.toString();
    }
}
