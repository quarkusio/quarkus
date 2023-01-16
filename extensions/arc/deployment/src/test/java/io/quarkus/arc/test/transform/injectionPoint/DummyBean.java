package io.quarkus.arc.test.transform.injectionPoint;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DummyBean {

    public String generateString() {
        return DummyBean.class.getSimpleName();
    }
}
