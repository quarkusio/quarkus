package io.quarkus.arc.test.interceptors.subclasses;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;

@ApplicationScoped
@MyBinding
public class SomeBean {

    public String foo() throws IOException, IOException {
        return "";
    }

    public String bar() {
        return "";
    }

}
