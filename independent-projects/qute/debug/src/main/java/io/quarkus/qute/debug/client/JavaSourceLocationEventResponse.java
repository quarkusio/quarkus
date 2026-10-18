package io.quarkus.qute.debug.client;

/**
 * Event response for a Java source location resolution.
 * <p>
 * Sent from an event-based client (VS Code, Eclipse IDE, etc.) back to the server
 * to complete the pending resolution of a Qute template to a Java source location.
 * </p>
 */
public class JavaSourceLocationEventResponse {

    /** The unique ID matching the original request/event. */
    private String id;

    /** The resolved Java source location corresponding to the template. */
    private JavaSourceLocationResponse response;

    public JavaSourceLocationEventResponse() {
    }

    public JavaSourceLocationEventResponse(String id, JavaSourceLocationResponse response) {
        this.id = id;
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JavaSourceLocationResponse getResponse() {
        return response;
    }

    public void setResponse(JavaSourceLocationResponse response) {
        this.response = response;
    }
}
