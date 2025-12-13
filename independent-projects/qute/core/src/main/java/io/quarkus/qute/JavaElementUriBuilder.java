package io.quarkus.qute;

import java.net.URI;

/**
 * Builder for Qute-specific URIs that reference a Java element
 * (class, method, or annotation) from a template.
 * <p>
 * These URIs have the format:
 *
 * <pre>
 * qute-java://&lt;fully-qualified-class-name&gt;[#method][@annotation]
 * </pre>
 *
 * Examples:
 * <ul>
 * <li>Class-level annotation: <code>qute-java://com.acme.Bean@io.quarkus.qute.TemplateContents</code></li>
 * <li>Method-level annotation: <code>qute-java://com.acme.Bean#process@io.quarkus.qute.TemplateContents</code></li>
 * </ul>
 * </p>
 *
 * <p>
 * This builder is used to construct such URIs in a type-safe way and to provide
 * utility methods to identify and parse them. It is aligned with
 * {@link io.quarkus.qute.debug.client.JavaSourceLocationArguments#javaElementUri}.
 * </p>
 */
public class JavaElementUriBuilder {

    /** Scheme used for Qute Java URIs. */
    public static final String QUTE_JAVA_SCHEME = "qute-java";

    /** Prefix for Qute Java URIs. */
    public static final String QUTE_JAVA_URI_PREFIX = QUTE_JAVA_SCHEME + "://";

    private final String typeName;
    private String method;
    private String annotation;

    private JavaElementUriBuilder(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Returns the fully qualified Java class name for this URI.
     *
     * @return the class name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Returns the Java method name (nullable if not specified).
     *
     * @return the method name or {@code null}
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the Java method name.
     *
     * @param method the method name to set
     * @return this builder
     */
    public JavaElementUriBuilder setMethod(String method) {
        this.method = method;
        return this;
    }

    /**
     * Returns the fully qualified Java annotation name (nullable if not specified).
     *
     * @return the annotation name or {@code null}
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Sets the fully qualified Java annotation name.
     *
     * @param annotation the annotation name to set
     * @return this builder
     */
    public JavaElementUriBuilder setAnnotation(String annotation) {
        this.annotation = annotation;
        return this;
    }

    /**
     * Creates a new builder for the given Java class name.
     *
     * @param typeName fully qualified Java class name
     * @return a new {@link JavaElementUriBuilder}
     */
    public static JavaElementUriBuilder builder(String typeName) {
        return new JavaElementUriBuilder(typeName);
    }

    /**
     * Builds the Qute Java URI representing the element.
     *
     * @return a {@link URI} for the Java element
     */
    public URI build() {
        StringBuilder uri = new StringBuilder(QUTE_JAVA_URI_PREFIX);
        uri.append(typeName);
        if (method != null) {
            uri.append("#").append(method);
        }
        if (annotation != null) {
            uri.append("@").append(annotation);
        }
        return URI.create(uri.toString());
    }

    /**
     * Returns true if the given URI uses the qute-java scheme.
     *
     * @param uri the URI to check
     * @return {@code true} if this URI is a Qute Java element URI
     */
    public static boolean isJavaUri(URI uri) {
        return uri != null && QUTE_JAVA_SCHEME.equals(uri.getScheme());
    }

    /**
     * Returns true if the given string starts with the qute-java URI prefix.
     *
     * @param uri the URI string to check
     * @return {@code true} if this string is a Qute Java element URI
     */
    public static boolean isJavaUri(String uri) {
        return uri != null && uri.startsWith(QUTE_JAVA_URI_PREFIX);
    }
}
