package io.quarkus.oidc.client.filter.runtime;

import java.util.function.Function;

import io.quarkus.runtime.util.StringUtil;
import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class OidcClientFilterConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.oidc-client-filter.";
    private static final String NEW_PREFIX = "quarkus.resteasy-client-oidc-filter.";
    private static final Function<String, String> RENAME_FUNCTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            return StringUtil.changePrefix(s, NEW_PREFIX, OLD_PREFIX);
        }
    };

    public OidcClientFilterConfigFallbackInterceptor() {
        super(RENAME_FUNCTION);
    }
}
