package io.quarkus.hibernate.validator.test.injection;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestInjectedBean {

    public List<String> allowedStrings() {
        return Collections.singletonList("Alpha");
    }

}
