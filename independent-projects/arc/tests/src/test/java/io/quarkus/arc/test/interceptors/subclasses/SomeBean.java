package io.quarkus.arc.test.interceptors.subclasses;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;

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
