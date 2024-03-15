package io.quarkus.csrf.reactive.runtime;

import java.util.function.Function;

import io.quarkus.runtime.util.StringUtil;
import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class CsrfReactiveConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {

    private static final String OLD_PREFIX = "quarkus.csrf-reactive.";
    private static final String NEW_PREFIX = "quarkus.rest-csrf.";
    private static final Function<String, String> RENAME_FUNCTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            return StringUtil.changePrefix(s, OLD_PREFIX, NEW_PREFIX);
        }
    };

    public CsrfReactiveConfigRelocateInterceptor() {
        super(RENAME_FUNCTION);
    }
}
