package io.quarkus.restclient.registerprovider;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MyRequestBean {
    int getUniqueNumber() {
        return System.identityHashCode(this);
    }
}
