package io.quarkus.arc.test.clientproxy.delegatingmethods;

public class Component implements HasElement {

    private static final long serialVersionUID = 1L;

    @Override
    public Object getElement() {
        return "an element";
    }
}
