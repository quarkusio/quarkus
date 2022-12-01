package io.quarkus.arc.test.clientproxy.delegatingmethods;

import java.io.Serializable;

@FunctionalInterface
public interface HasElement extends Serializable {

    Object getElement();

}
