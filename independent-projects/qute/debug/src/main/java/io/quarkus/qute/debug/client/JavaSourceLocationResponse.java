package io.quarkus.qute.debug.client;

/**
 * Response containing the location of a Java method, class, or template content
 * referenced from a Qute template.
 * <p>
 * Typically returned by {@link JavaSourceResolver#resolveJavaSource}.
 * </p>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * qute-java://org.acme.quarkus.sample.HelloResource$Hello@io.quarkus.qute.TemplateContents
 * → javaFileUri = "file:///project/src/main/java/org/acme/quarkus/sample/HelloResource.java"
 * → startLine = 16
 * </pre>
 *
 * <p>
 * The {@code startLine} represents the line where the template content or the
 * Java element declaration starts. Lines are 1-based in this API (first line =
 * 1).
 * </p>
 */
public class JavaSourceLocationResponse {

    /** URI of the Java source file containing the resolved element. */
    private String javaFileUri;

    /**
     * Start line of the Java element or template content in the file.
     * <p>
     * 1-based index of the line where the element or template content starts.
     * </p>
     */
    private int startLine;

    public JavaSourceLocationResponse() {
    }

    public JavaSourceLocationResponse(String javaFileUri, int startLine) {
        setJavaFileUri(javaFileUri);
        setStartLine(startLine);
    }

    /**
     * Returns the URI of the Java source file containing the resolved element.
     *
     * @return the file URI
     */
    public String getJavaFileUri() {
        return javaFileUri;
    }

    /**
     * Sets the URI of the Java source file containing the resolved element.
     *
     * @param javaFileUri the file URI to set
     */
    public void setJavaFileUri(String javaFileUri) {
        this.javaFileUri = javaFileUri;
    }

    /**
     * Returns the start line of the Java element or template content in the source
     * file.
     *
     * @return 1-based start line of the element or template content
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Sets the start line of the Java element or template content in the source
     * file.
     *
     * @param startLine 1-based start line of the element or template content
     */
    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }
}
