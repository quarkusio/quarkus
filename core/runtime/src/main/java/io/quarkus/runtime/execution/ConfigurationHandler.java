package io.quarkus.runtime.execution;

import java.io.Serializable;

/**
 * An execution handler which loads the initial configuration.
 */
public final class ConfigurationHandler implements ExecutionHandler, Serializable {
    private static final long serialVersionUID = 1929067036940581640L;

    private static final ConfigurationHandler INSTANCE = new ConfigurationHandler();

    private ConfigurationHandler() {
    }

    public static ConfigurationHandler getInstance() {
        return INSTANCE;
    }

    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ConfigurationHandler.class.getClassLoader();
        }
        Class.forName("io.quarkus.runtime.generated.RunTimeConfig", true, cl).getDeclaredMethod("getRunTimeConfiguration")
                .invoke(null);
        return chain.proceed(context);
    }
}
