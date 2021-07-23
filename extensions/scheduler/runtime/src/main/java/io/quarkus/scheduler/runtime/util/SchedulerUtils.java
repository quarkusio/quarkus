package io.quarkus.scheduler.runtime.util;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;

/**
 * Utilities class for scheduler extensions.
 *
 */
public class SchedulerUtils {

    private static final String DELAYED = "delayed";
    private static final String EVERY = "every";

    private SchedulerUtils() {

    }

    /**
     * Parse the `@Scheduled(delayed = "")` field into milliseconds.
     *
     * @param scheduled annotation
     * @return returns the duration in milliseconds.
     */
    public static long parseDelayedAsMillis(Scheduled scheduled) {
        String value = lookUpPropertyValue(scheduled.delayed());
        return parseDurationAsMillis(scheduled, value, DELAYED);
    }

    /**
     * Parse the `@Scheduled(every = "")` field into milliseconds.
     *
     * @param scheduled annotation
     * @return returns the duration in milliseconds or {@link OptionalLong#empty()} if the expression evaluates to "off" or
     *         "disabled".
     */
    public static OptionalLong parseEveryAsMillis(Scheduled scheduled) {
        String value = lookUpPropertyValue(scheduled.every());
        OptionalLong optionalMillis = OptionalLong.empty();
        if (!isOff(value)) {
            optionalMillis = OptionalLong.of(parseDurationAsMillis(scheduled, value, EVERY));
        }
        return optionalMillis;
    }

    public static boolean isOff(String value) {
        return value != null && (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("disabled"));
    }

    /**
     * Looks up the property value by checking whether the value is a configuration key and resolves it if so.
     *
     * @param propertyValue property value to look up.
     * @return the resolved property value.
     */
    public static String lookUpPropertyValue(String propertyValue) {
        String value = propertyValue.stripLeading();
        if (!value.isEmpty() && isConfigValue(value)) {
            value = resolvePropertyExpression(adjustExpressionSyntax(value));
        }
        return value;
    }

    public static boolean isConfigValue(String val) {
        return isSimpleConfigValue(val) || isConfigExpression(val);
    }

    private static boolean isSimpleConfigValue(String val) {
        val = val.trim();
        return val.startsWith("{") && val.endsWith("}");
    }

    /**
     * Converts "{property}" to "${property}" for backwards compatibility
     */
    private static String adjustExpressionSyntax(String val) {
        if (isSimpleConfigValue(val)) {
            return '$' + val;
        }
        return val;
    }

    /**
     * Adapted from {@link io.smallrye.config.ExpressionConfigSourceInterceptor}
     */
    private static String resolvePropertyExpression(String expr) {
        // Force the runtime CL in order to make the DEV UI page work  
        final ClassLoader cl = SchedulerUtils.class.getClassLoader();
        final Config config = ConfigProviderResolver.instance().getConfig(cl);
        final Expression expression = Expression.compile(expr, LENIENT_SYNTAX, NO_TRIM);
        final String expanded = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(ResolveContext<RuntimeException> resolveContext, StringBuilder stringBuilder) {
                final Optional<String> resolve = config.getOptionalValue(resolveContext.getKey(), String.class);
                if (resolve.isPresent()) {
                    stringBuilder.append(resolve.get());
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    throw new NoSuchElementException(String.format("Could not expand value %s in property %s",
                            resolveContext.getKey(), expr));
                }
            }
        });
        return expanded;
    }

    private static boolean isConfigExpression(String val) {
        if (val == null) {
            return false;
        }
        int exprStart = val.indexOf("${");
        int exprEnd = -1;
        if (exprStart >= 0) {
            exprEnd = val.indexOf('}', exprStart + 2);
        }
        return exprEnd > 0;
    }

    private static long parseDurationAsMillis(Scheduled scheduled, String value, String memberName) {
        return Math.abs(parseDuration(scheduled, value, memberName).toMillis());
    }

    private static Duration parseDuration(Scheduled scheduled, String value, String memberName) {
        if (Character.isDigit(value.charAt(0))) {
            value = "PT" + value;
        }

        try {
            return Duration.parse(value);
        } catch (Exception e) {
            // This could only happen for config-based expressions
            throw new IllegalStateException("Invalid " + memberName + "() expression on: " + scheduled, e);
        }
    }
}
