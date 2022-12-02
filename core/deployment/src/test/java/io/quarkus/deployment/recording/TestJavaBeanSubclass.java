package io.quarkus.deployment.recording;

public class TestJavaBeanSubclass extends TestJavaBean {
    private volatile String method;

    public TestJavaBeanSubclass() {
    }

    public TestJavaBeanSubclass(String sval, int ival, final String method) {
        super(sval, ival);
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public TestJavaBeanSubclass setMethod(final String method) {
        this.method = method;
        return this;
    }
}
