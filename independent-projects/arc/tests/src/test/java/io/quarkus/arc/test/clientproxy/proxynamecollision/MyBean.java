package io.quarkus.arc.test.clientproxy.proxynamecollision;

public class MyBean {

    private String source;

    public MyBean() {
    }

    public MyBean(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
