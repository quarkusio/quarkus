package io.quarkus.load.shedding.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ManagementRequestPrioritizer implements RequestPrioritizer<RoutingContext> {
    // the non-application root path with no trailing `/`, or `null` if non-application
    // endpoints cannot be told apart from application endpoints by their path
    private final String managementPath;

    // the same with a trailing `/`, precomputed so that matching allocates nothing
    private final String managementPathPrefix;

    @Inject
    public ManagementRequestPrioritizer(
            VertxHttpBuildTimeConfig buildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        String path = managementBuildTimeConfig.enabled()
                // non-application endpoints are served by the separate management interface,
                // which has a router of its own and is not subject to load shedding
                ? null
                : nonApplicationPath(buildTimeConfig.rootPath(), buildTimeConfig.nonApplicationRootPath());
        managementPath = path;
        managementPathPrefix = path == null ? null : path + "/";
    }

    /**
     * Returns the non-application root path with no trailing {@code /}, or {@code null} if
     * non-application endpoints share the root path with the application and therefore cannot
     * be recognized.
     * <p>
     * {@code rootPath} is normalized and always has both a leading and a trailing {@code /};
     * {@code nonApplicationRootPath} is not normalized at all. It is absolute when it starts
     * with a {@code /} and relative to the root path otherwise.
     */
    static String nonApplicationPath(String rootPath, String nonApplicationRootPath) {
        String root = stripTrailingSlashes(rootPath);
        String result = nonApplicationRootPath.startsWith("/")
                ? nonApplicationRootPath
                : root + "/" + nonApplicationRootPath;
        result = stripTrailingSlashes(result);

        // setting the non-application root path to the root path disables it, and then every
        // endpoint is an application endpoint. Both values are compared after normalization,
        // because the root path has a trailing `/` and the value configured to match it
        // typically does not
        if (result.isEmpty() || result.equals(root)) {
            return null;
        }
        return result;
    }

    private static String stripTrailingSlashes(String path) {
        int end = path.length();
        while (end > 0 && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(0, end);
    }

    @Override
    public boolean appliesTo(Object request) {
        if (managementPath != null && request instanceof RoutingContext ctx) {
            // a plain `startsWith` would also match an application endpoint that happens to
            // begin with the same characters, such as `/queries` under the default `/q`
            String path = ctx.normalizedPath();
            return path.equals(managementPath) || path.startsWith(managementPathPrefix);
        }
        return false;
    }

    @Override
    public RequestPriority priority(RoutingContext request) {
        return RequestPriority.NEVER;
    }
}
