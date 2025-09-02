package io.quarkus.test.common.http;

import static io.quarkus.test.common.http.TestHTTPConfigSourceProvider.HTTP_ROOT_PATH_KEY;
import static io.quarkus.test.common.http.TestHTTPConfigSourceProvider.MANAGEMENT_ROOT_PATH_KEY;
import static io.quarkus.test.common.http.TestHTTPConfigSourceProvider.TEST_MANAGEMENT_URL_KEY;
import static io.quarkus.test.common.http.TestHTTPConfigSourceProvider.TEST_MANAGEMENT_URL_SSL_KEY;
import static io.quarkus.test.common.http.TestHTTPConfigSourceProvider.TEST_URL_KEY;
import static io.quarkus.test.common.http.TestHTTPConfigSourceProvider.TEST_URL_SSL_KEY;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.Priorities;

/**
 * Override the expression expansion for test urls so they can be sanitized. A simple interceptor does not work
 * because the test urls values are nested expressions, so when the default expression interceptor runs, either we get
 * the full value expanded or the value unexpanded. In most cases, the test urls are used as expressions, so we need to
 * intercept the expression expansion directly to rewrite what we need.
 */
@Priority(Priorities.LIBRARY + 299)
public class TestHTTPConfigSourceInterceptor extends ExpressionConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (name.equals(TEST_URL_KEY) ||
                name.equals(TEST_MANAGEMENT_URL_KEY) ||
                name.equals(TEST_URL_SSL_KEY) ||
                name.equals(TEST_MANAGEMENT_URL_SSL_KEY)) {

            return sanitizeUrl(super.getValue(context, name));
        } else if (name.equals(HTTP_ROOT_PATH_KEY) || name.equals(MANAGEMENT_ROOT_PATH_KEY)) {
            ConfigValue configValue = super.getValue(context, name);
            if ((configValue == null) || (configValue.getRawValue() == null) || configValue.getRawValue().isEmpty()
                    || configValue.getRawValue().startsWith("/")) {
                return configValue;
            }
            return configValue.from().withValue("/" + configValue.getValue()).withRawValue("/" + configValue.getRawValue())
                    .build();
        }

        return context.proceed(name);
    }

    private static ConfigValue sanitizeUrl(ConfigValue configValue) {
        if (configValue == null || configValue.getValue() == null) {
            return configValue;
        }

        String url = configValue.getValue();
        url = url.replace("0.0.0.0", "localhost");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return configValue.from().withValue(url).build();
    }
}
