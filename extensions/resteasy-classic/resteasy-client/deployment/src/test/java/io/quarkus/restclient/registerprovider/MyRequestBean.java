package io.quarkus.restclient.registerprovider;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class MyRequestBean {
    int getUniqueNumber() {
        return System.identityHashCode(this);
    }
}
