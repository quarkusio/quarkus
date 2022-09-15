package io.quarkus.arc.test.clientproxy.delegatingmethods;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyBean extends Component implements HasSize {

    private static final long serialVersionUID = 1L;

    public MyBean() {
        setSize("5px");
    }

}
