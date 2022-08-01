package io.quarkus.arc.test.interceptors.bindings.transitive;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@NotABinding
public class NotInterceptedBean {

    public void ping() {

    }
}
