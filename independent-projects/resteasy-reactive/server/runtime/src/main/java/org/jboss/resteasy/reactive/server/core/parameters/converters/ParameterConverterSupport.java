package org.jboss.resteasy.reactive.server.core.parameters.converters;

/**
 * This class isn't used directly, it is however used by generated code meant to deal with {@link ParameterConverter}.
 */
@SuppressWarnings("unused")
public final class ParameterConverterSupport {

    private ParameterConverterSupport() {
    }

    /**
     * Normally the reflective instantiation would not be needed, and we could just instantiate normally,
     * however that could break dev-mode when the converters are in a different module and non-standard Maven
     * configuration is used (see <a href="https://github.com/quarkusio/quarkus/issues/39773#issuecomment-2030493539">this</a>)
     */
    public static ParameterConverter create(String className) {
        try {
            Class<?> clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return (ParameterConverter) clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create instance of " + className, e);
        }
    }
}
