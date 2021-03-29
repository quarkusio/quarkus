package io.quarkus.dev;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The generators can be used to generate a custom HTML page for a specific deployment exception that occurs during the
 * development mode.
 * <p>
 * In order to avoid classloading issues the generators should not access the root cause directly but use reflection instead
 * (the exception class could be loaded by a different class loader).
 */
public class ErrorPageGenerators {

    private static final Map<String, Function<Throwable, String>> generators = new ConcurrentHashMap<>();

    /**
     * Register a function that will be used to generate the error page for the given root cause.
     * 
     * @param rootCauseClassName
     * @param function
     */
    public static void register(String rootCauseClassName, Function<Throwable, String> function) {
        if (generators.putIfAbsent(rootCauseClassName, function) != null) {
            throw new IllegalStateException("Template builder already specified for: " + rootCauseClassName);
        }
    }

    public static Function<Throwable, String> get(String rootCauseClassName) {
        return generators.get(rootCauseClassName);
    }

    // This method is called by a relevant service provider during HotReplacementSetup#close()
    public static void clear() {
        generators.clear();
    }

}
