package io.quarkus.arc.test.clientproxy.delegatingmethods;

public interface HasSize extends HasElement {

    default void setSize(String size) {
        getElement();
    }
}
