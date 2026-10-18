package io.quarkus.qute.debug.client;

/**
 * Arguments describing a Java element referenced from a Qute template.
 * <p>
 * Sent by the Debug Adapter via {@link JavaSourceResolver#resolveJavaSource}
 * to the client in order to locate the corresponding Java source file and position.
 * </p>
 *
 * <h2>Example of a qute-java URI</h2>
 *
 * <pre>
 * qute-java://com.acme.Bean#process@io.quarkus.qute.TemplateContents
 * </pre>
 *
 * <p>
 * Interpretation:
 * </p>
 * <ul>
 * <li>javaElementUri = "qute-java://com.acme.Bean#process@io.quarkus.qute.TemplateContents"</li>
 * <li>typeName = "com.acme.Bean"</li>
 * <li>method = "process" (optional; if null, the annotation is applied on the class)</li>
 * <li>annotation = "io.quarkus.qute.TemplateContents"</li>
 * </ul>
 */
public class JavaSourceLocationArguments {

    /** The qute-java URI used to locate the Java element from a template. */
    private String javaElementUri;

    /** Fully qualified Java class, interface name (e.g., "com.acme.Bean"). */
    private String typeName;

    /**
     * Java method name.
     * <p>
     * Optional: if {@code null}, the annotation is applied to the class itself.
     * </p>
     */
    private String method;

    /** Fully qualified Java annotation name (typically "io.quarkus.qute.TemplateContents"). */
    private String annotation;

    /**
     * Returns the qute-java URI used to locate the Java element.
     *
     * @return the URI string
     */
    public String getJavaElementUri() {
        return javaElementUri;
    }

    /**
     * Sets the qute-java URI used to locate the Java element.
     *
     * @param javaElementUri the URI string to set
     */
    public void setJavaElementUri(String javaElementUri) {
        this.javaElementUri = javaElementUri;
    }

    /**
     * Returns the fully qualified Java class, interface name.
     *
     * @return the class name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets the fully qualified Java class, interface name.
     *
     * @param typeName the class, interface name to set
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Returns the Java method name.
     *
     * @return the method name, or {@code null} if the annotation applies to the class
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the Java method name.
     *
     * @param method the method name to set, or {@code null} if the annotation applies to the class
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the fully qualified Java annotation name.
     *
     * @return the annotation name (e.g., "io.quarkus.qute.TemplateContents")
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Sets the fully qualified Java annotation name.
     *
     * @param annotation the annotation name to set
     */
    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}
