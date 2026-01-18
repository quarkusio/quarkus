package io.quarkus.qute.debug.client;

import java.util.Objects;

/**
 * Payload sent by the server to event-based clients (VS Code, etc.) when
 * requesting the resolution of a Java source corresponding to a Qute template.
 *
 * <p>
 * Contains a unique request ID to match the response and the original
 * {@link JavaSourceLocationArguments} describing the template location.
 * </p>
 */
public class JavaSourceLocationEventArguments {

    /**
     * Unique ID of the request. Used to match the response from the client to the
     * pending CompletableFuture on the server.
     */
    private String id;

    /**
     * Original Java source location arguments parsed from the qute-java:// URI.
     */
    private JavaSourceLocationArguments args;

    public JavaSourceLocationEventArguments() {
    }

    public JavaSourceLocationEventArguments(String id, JavaSourceLocationArguments args) {
        this.id = id;
        this.args = args;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JavaSourceLocationArguments getArgs() {
        return args;
    }

    public void setArgs(JavaSourceLocationArguments args) {
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JavaSourceLocationEventArguments that))
            return false;
        return Objects.equals(id, that.id) && Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, args);
    }

    @Override
    public String toString() {
        return "JavaSourceLocationRequestArguments{" + "id='" + id + '\'' + ", args=" + args + '}';
    }
}
