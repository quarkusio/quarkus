package io.quarkus.it.smallrye.graphql.metricresources;

/**
 * Just a test pojo
 */
public class TestPojo {
    private String message;

    public TestPojo() {
        super();
    }

    public TestPojo(String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
