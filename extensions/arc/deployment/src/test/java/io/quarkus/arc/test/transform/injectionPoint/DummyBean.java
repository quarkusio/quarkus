package io.quarkus.arc.test.transform.injectionPoint;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DummyBean {

    public String generateString() {
        return DummyBean.class.getSimpleName();
    }
}
