package io.quarkus.test.common;

/**
 * {@link ClassValue} are used in Groovy which causes memory leaks if not properly cleaned, but unfortunately, they are
 * very complex to clean up, especially the {@link ClassValue} corresponding to system classes that must be cleaned too
 * to avoid memory leaks moreover if not cleaned wisely errors of type {@code MissingMethodException} can be thrown, so
 * we had better to simply disable them by setting the System property {@code groovy.use.classvalue} to {@code false}
 * see <a href="https://issues.apache.org/jira/browse/GROOVY-7591">GROOVY-7591</a> for more details.
 */
public final class GroovyClassValue {

    private GroovyClassValue() {
    }

    public static void disable() {
        System.setProperty("groovy.use.classvalue", "false");
    }
}
