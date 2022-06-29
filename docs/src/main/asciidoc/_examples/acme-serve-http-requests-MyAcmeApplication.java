// tag::application[]
package org.acme;

import javax.enterprise.context.ApplicationScoped;

import org.acme.corp.Anvil;
import org.acme.corp.Toaster;

import io.smallrye.mutiny.Multi;

@ApplicationScoped // <1>
public class MyAcmeApplication {

    @Anvil(optional = {"name"}) // <2>
    public String hello(String name) {
        return String.format("Hello, %s!", (name == null ? "World" : name));
    }

    // tag::goodbye[]
    @Toaster // <1>
    public Multi<String> longGoodbye(String name) {
        return Multi.createFrom().items("Goodbye", ",", "Sweet", "Planet", "!"); // <2>
    }
    // end::goodbye[]
}
// end::application[]